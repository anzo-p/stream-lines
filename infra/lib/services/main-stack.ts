import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import { Construct } from 'constructs';
// import { AlbStack } from './alb-stack';
import { AutoTeardownStack } from './auto-teardown';
// import { BackendStack } from './backend-stack';
// import { DashboardStack } from './dashboard-stack';
import { EcsTaskExecutionRole } from './ecs-task-exec-role';
import { GatherStack } from './gather-stack';
import { IngestStack } from './ingest-stack';
import { KinesisStreamsStack } from './kinesis-stack';
import { RipplesStack } from './ripples-stack';
// import { WebSocketApiGatewayStack } from './api-gateway-stack';
import { CurrentsStack } from './currents-stack';
import { NatGatewayStack } from './nat-gateway-stack';

interface ServicesStackProps extends cdk.StackProps {
  vpc: ec2.Vpc;
  securityGroups: Record<string, ec2.SecurityGroup>;
  ecsCluster: ecs.Cluster;
}

export class ServicesStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: ServicesStackProps) {
    super(scope, id, props);

    const { vpc, securityGroups, ecsCluster } = props;

    const autoTeardownDenied = this.node.tryGetContext('autoTeardown') === 'false';
    const runOnlyOnDemandServices = this.node.tryGetContext('onlyOnDemand') === 'true';
    const runAllServicesOnDemand = this.node.tryGetContext('runAllAsOnDemand') === 'true';

    /*
    const wsApigatewayStack = new WebSocketApiGatewayStack(
      this,
      'ApiGatewayStack'
    );
    */

    const kinesisStack = new KinesisStreamsStack(
      this,
      'KinesisStack'
      //wsApigatewayStack.wsApiGatewayStageProdArn,
      //wsApigatewayStack.wsApiGatewayConnectionsUrl
    );

    // const albStack = new AlbStack(this, 'AlbStack', vpc);

    const taskExecRoleStack = new EcsTaskExecutionRole(this, 'StreamLinesEcsTaskExecRole');

    new NatGatewayStack(this, 'NatGatewayStack', vpc);

    let gatherStack: GatherStack | undefined;
    if (!runOnlyOnDemandServices || runAllServicesOnDemand) {
      gatherStack = new GatherStack(this, 'GatherStack', {
        bastionSecurityGroup: securityGroups['bastion'],
        connectingServiceSGs: ['ingest'].map((id) => ({ id, sg: securityGroups[id] })),
        desiredCount: 1,
        ecsCluster,
        executionRole: taskExecRoleStack.role,
        runAsOndemand: runAllServicesOnDemand,
        securityGroup: securityGroups['gather']
      });

      new CurrentsStack(this, 'CurrentsStack', {
        desiredCount: 1,
        ecsCluster,
        executionRole: taskExecRoleStack.role,
        securityGroup: securityGroups['currents'],
        runAsOndemand: runAllServicesOnDemand
      });
    }

    const ripplesStack = new RipplesStack(this, 'RipplesStack', {
      desiredCount: 1,
      ecsCluster,
      executionRole: taskExecRoleStack.role,
      securityGroup: securityGroups['ripples'],
      runAsOndemand: true,
      readKinesisUpstreamPerms: kinesisStack.readUpstreamPerms,
      writeKinesisDownStreamPerms: kinesisStack.writeDownstreamPerms
    });
    ripplesStack.addDependency(kinesisStack);

    const ingestStack = new IngestStack(this, 'IngestStack', {
      desiredCount: 1,
      ecsCluster,
      executionRole: taskExecRoleStack.role,
      securityGroup: securityGroups['ingest'],
      runAsOndemand: true,
      writeKinesisUpstreamPerms: kinesisStack.writeUpstreamPerms
    });
    ingestStack.addDependency(kinesisStack);
    if (gatherStack) ingestStack.addDependency(gatherStack);

    /*
    const backendStack = new BackendStack(
      this,
      'BackendStack',
      ecsCluster.ecsCluster,
      taskExecRoleStack.role,
      backendSecurityGroup,

      albStack.backendAlbListener
    );
    backendStack.addDependency(wsApigatewayStack);

    const dashboardStack = new DashboardStack(
      this,
      'DashboardStack',
      ecsCluster.ecsCluster,
      taskExecRoleStack.role,
      albStack.dashboardAlbListener
    );
    dashboardStack.addDependency(wsApigatewayStack);
    dashboardStack.addDependency(backendStack);
    */

    if (!autoTeardownDenied) {
      new AutoTeardownStack(this, 'AutoTeardownStack', {
        targetStackName: cdk.Stack.of(this).stackName,
        targetStackArn: cdk.Stack.of(this).stackId
      });
    }
  }
}
