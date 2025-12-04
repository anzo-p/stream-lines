import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';
// import { AlbStack } from './alb-stack';
// import { BackendStack } from './backend-stack';
// import { DashboardStack } from './dashboard-stack';
import { EcsClusterStack } from './ecs-cluster-stack';
import { EcsTaskExecutionRole } from './ecs-task-exec-role';
import { GatherStack } from './gather-stack';
import { InfluxDbHostStack as InfluxDbStack } from './influxdb-ec2-stack';
import { IngestStack } from './ingest-stack';
import { KinesisStreamsStack } from './kinesis-stack';
import { RipplesStack } from './ripples-stack';
import { VpcStack } from './vpc-stack';
// import { WebSocketApiGatewayStack } from './api-gateway-stack';
import { JumpBastionStack } from './jump-bastion-stack';

export class AppInfraStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const vpcStack = new VpcStack(this, 'VpcStack');

    /*
    const wsApigatewayStack = new WebSocketApiGatewayStack(
      this,
      'ApiGatewayStack'
    );
    */

    const kinesisStack = new KinesisStreamsStack(
      this,
      'KinesisStack',
      //wsApigatewayStack.wsApiGatewayStageProdArn,
      //wsApigatewayStack.wsApiGatewayConnectionsUrl
    );

    // const albStack = new AlbStack(this, 'AlbStack', vpcStack.vpc);

    const taskExecRoleStack = new EcsTaskExecutionRole(
      this,
      'StreamLinesEcsTaskExecRole'
    );

    const ecsCluster = new EcsClusterStack(
      this,
      'EcsClusterStack',
      vpcStack.vpc,
    );

    const bastionSecurityGroup = new ec2.SecurityGroup(this, 'BastionSecurityGroup', {
      vpc: vpcStack.vpc,
      allowAllOutbound: true,
    });

    const influxDbSecurityGroup = new ec2.SecurityGroup(
      this,
      'InfluxDbServiceSecurityGroup',
      {
        vpc: vpcStack.vpc,
        allowAllOutbound: true,
      });

    const ingestSecurityGroup = new ec2.SecurityGroup(
      this,
      'IngestSecurityGroup',
      {
        vpc: vpcStack.vpc,
        allowAllOutbound: true,
      });

    const ripplesServiceSecurityGroup = new ec2.SecurityGroup(
      this,
      'RipplesSecurityGroup',
      {
        vpc: vpcStack.vpc,
        allowAllOutbound: true
      }
    );

    const gatherSecurityGroup = new ec2.SecurityGroup(
      this,
      'GatherSecurityGroup',
      {
        vpc: vpcStack.vpc,
        allowAllOutbound: true,
      });


    /*
    const backendSecurityGroup = new ec2.SecurityGroup(
      this,
      'BackendSecurityGroup',
      {
        vpc: vpcStack.vpc,
        allowAllOutbound: true,
      });
    */

    // 1. appears best to deploy vpc and everythiung up to influx and its bastion first
    // 2. then manage influxdb buckets and those tokens through web ui via bastion
    // 3. then deploy gather, which then serves out the tickers for ingest
    // 4. then ripples, ingest and currents, and now everything is up to influx dasboards
    // 5. finally, optionally, backend and dashboard for ui

    new JumpBastionStack(
      this,
      'JumpBastionStack',
      vpcStack.vpc,
      bastionSecurityGroup
    );

    const influxDbStack = new InfluxDbStack(
      this,
      'InfluxDbHostStack',
      vpcStack.vpc,
      ecsCluster.ecsCluster,
      influxDbSecurityGroup,
      bastionSecurityGroup,
      [
        { key: 'ripples', sg: ripplesServiceSecurityGroup },
        { key: 'gather', sg: gatherSecurityGroup },
        //{ key: 'backend', sg: backendSecurityGroup },
      ]
    );

    const gatherStack = new GatherStack(
      this,
      'GatherStack',
      ecsCluster.ecsCluster,
      taskExecRoleStack.role,
      gatherSecurityGroup,
      [
        { key: 'ingest', sg: ingestSecurityGroup },
      ]
    );
    gatherStack.addDependency(influxDbStack);

    const ripplesStack = new RipplesStack(
      this,
      'ripplesStack',
      ecsCluster.ecsCluster,
      taskExecRoleStack.role,
      ripplesServiceSecurityGroup,
      kinesisStack.readUpstreamPerms,
      kinesisStack.writeDownstreamPerms
    );
    ripplesStack.addDependency(kinesisStack);
    ripplesStack.addDependency(influxDbStack);

    const ingestStack = new IngestStack(
      this,
      'IngestStack',
      ecsCluster.ecsCluster,
      taskExecRoleStack.role,
      ingestSecurityGroup,
      kinesisStack.writeUpstreamPerms
    );
    ingestStack.addDependency(kinesisStack);
    ingestStack.addDependency(gatherStack);

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
    backendStack.addDependency(influxDbStack);

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
  }
}
