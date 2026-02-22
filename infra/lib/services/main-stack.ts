import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import { Construct } from 'constructs';
import { AlbStack } from './alb-stack';
import { AutoTeardownStack } from './auto-teardown';
import { BackendStack } from './backend-stack';
import { CurrentsStack } from './currents-stack';
// import { DashboardStack } from './dashboard-stack';
import { DrawdownSagemakerStack } from './drawdown-sagemaker-stack';
import { EcsTaskExecutionRole } from './ecs-task-exec-role';
import { GatherStack } from './gather-stack';
import { IngestStack } from './ingest-stack';
import { KinesisStreamsStack } from './kinesis-stack';
import { NarwhalStack } from './narwhal-stack';
import { NatGatewayStack } from './nat-gateway-stack';
import { RipplesStack } from './ripples-stack';
import { WebSocketApiGatewayStack } from './api-gateway-stack';

interface ServicesStackProps extends cdk.StackProps {
  vpc: ec2.Vpc;
  ecsCluster: ecs.Cluster;
}

export class ServicesStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: ServicesStackProps) {
    super(scope, id, props);

    const { vpc, ecsCluster } = props;

    const influxSg = ec2.SecurityGroup.fromSecurityGroupId(
      this,
      'ImportedInfluxSg',
      cdk.Fn.importValue('StreamLines-Infra:InfluxSgId'),
      { mutable: true }
    );

    const backendSg = new ec2.SecurityGroup(this, 'BackendSecurityGroup', { vpc, allowAllOutbound: true });
    const currentsSg = new ec2.SecurityGroup(this, 'CurrentsSecurityGroup', { vpc, allowAllOutbound: true });
    const gatherSg = new ec2.SecurityGroup(this, 'GatherSecurityGroup', { vpc, allowAllOutbound: true });
    const ingestSg = new ec2.SecurityGroup(this, 'IngestSecurityGroup', { vpc, allowAllOutbound: true });
    const narwhalSg = new ec2.SecurityGroup(this, 'NarwhalSecurityGroup', { vpc, allowAllOutbound: true });
    const ripplesSg = new ec2.SecurityGroup(this, 'RipplesSecurityGroup', { vpc, allowAllOutbound: true });

    [currentsSg, gatherSg, ingestSg, narwhalSg, ripplesSg].forEach((sg) => {
      influxSg.connections.allowFrom(
        sg,
        ec2.Port.tcp(Number(process.env.INFLUXDB_SERVER_PORT!)),
        `${sg.node.id}-to-Influx`
      );
    });

    const autoTeardownDenied = this.node.tryGetContext('autoTeardown') === 'false';
    const runOnlyOnDemandServices = this.node.tryGetContext('onlyOnDemand') === 'true';
    const runAllServicesOnDemand = this.node.tryGetContext('runAllAsOnDemand') === 'true';

    const wsApigatewayStack = new WebSocketApiGatewayStack(this, 'ApiGatewayStack');

    const kinesisStack = new KinesisStreamsStack(
      this,
      'KinesisStack',
      wsApigatewayStack.wsApiGatewayStageProdArn,
      wsApigatewayStack.wsApiGatewayConnectionsUrl
    );

    const albStack = new AlbStack(this, 'AlbStack', vpc);

    const taskExecRoleStack = new EcsTaskExecutionRole(this, 'StreamLinesEcsTaskExecRole');

    new NatGatewayStack(this, 'NatGatewayStack', vpc);

    let gatherStack: GatherStack | undefined;
    if (!runOnlyOnDemandServices || runAllServicesOnDemand) {
      gatherStack = new GatherStack(this, 'GatherStack', {
        desiredCount: 1,
        ecsCluster,
        executionRole: taskExecRoleStack.role,
        runAsOndemand: runAllServicesOnDemand,
        securityGroup: gatherSg
      });

      new CurrentsStack(this, 'CurrentsStack', {
        desiredCount: 1,
        ecsCluster,
        executionRole: taskExecRoleStack.role,
        securityGroup: currentsSg,
        runAsOndemand: runAllServicesOnDemand
      });
    }

    const ripplesStack = new RipplesStack(this, 'RipplesStack', {
      desiredCount: 1,
      ecsCluster,
      executionRole: taskExecRoleStack.role,
      securityGroup: ripplesSg,
      runAsOndemand: true,
      readKinesisUpstreamPerms: kinesisStack.readUpstreamPerms,
      writeKinesisDownStreamPerms: kinesisStack.writeDownstreamPerms
    });
    ripplesStack.addDependency(kinesisStack);

    const ingestStack = new IngestStack(this, 'IngestStack', {
      desiredCount: 1,
      ecsCluster,
      executionRole: taskExecRoleStack.role,
      securityGroup: ingestSg,
      runAsOndemand: true,
      writeKinesisUpstreamPerms: kinesisStack.writeUpstreamPerms
    });
    ingestStack.addDependency(kinesisStack);
    if (gatherStack) ingestStack.addDependency(gatherStack);

    const backendStack = new BackendStack(this, 'BackendStack', {
      backendAlbListener: albStack.backendAlbListener,
      ecsCluster,
      ecsTaskExecRole: taskExecRoleStack.role,
      securityGroup: backendSg
    });
    backendStack.addDependency(wsApigatewayStack);

    /*
    // should frontend be run out of S3 via CloudFront?
    const dashboardStack = new DashboardStack(this, 'DashboardStack', {
      dashboardAlbListener: albStack.dashboardAlbListener,
      ecsCluster,
      ecsTaskExecRole: taskExecRoleStack.role
    });
    dashboardStack.addDependency(wsApigatewayStack);
    dashboardStack.addDependency(backendStack);
    */

    new NarwhalStack(this, 'NarwhalStack', {
      desiredCount: 1,
      ecsCluster,
      executionRole: taskExecRoleStack.role,
      runAsOndemand: runAllServicesOnDemand,
      securityGroup: narwhalSg
    });

    new DrawdownSagemakerStack(this, 'SagemakerStack');

    if (!autoTeardownDenied) {
      new AutoTeardownStack(this, 'AutoTeardownStack', {
        targetStackName: cdk.Stack.of(this).stackName,
        targetStackArn: cdk.Stack.of(this).stackId
      });
    }
  }
}
