import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { AlbStack } from './alb-stack';
import { BackendStack } from './backend-stack';
import { DashboardStack } from './dashboard-stack';
import { EcsClusterStack } from './ecs-cluster-stack';
import { EcsTaskExecutionRole } from './ecs-task-exec-role';
import { InfluxDbStack } from './influxdb-stack';
import { IngestStack } from './ingest-stack';
import { KinesisStreamsStack } from './kinesis-stack';
import { RipplesStack } from './ripples-stack';
import { VpcStack } from './vpc-stack';
import { WebSocketApiGatewayStack } from './api-gateway-stack';

export class AppInfraStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const vpcStack = new VpcStack(this, 'VpcStack');

    const wsApigatewayStack = new WebSocketApiGatewayStack(
      this,
      'ApiGatewayStack'
    );

    const kinesisStack = new KinesisStreamsStack(
      this,
      'KinesisStack',
      wsApigatewayStack.wsApiGatewayStageProdArn,
      wsApigatewayStack.wsApiGatewayConnectionsUrl
    );

    const ecsCluster = new EcsClusterStack(
      this,
      'EcsClusterStack',
      vpcStack.vpc
    );

    const albStack = new AlbStack(this, 'AlbStack', vpcStack.vpc);

    const taskExecRoleStack = new EcsTaskExecutionRole(
      this,
      'StreamLinesEcsTaskExecRole'
    );

    const influxStack = new InfluxDbStack(
      this,
      'InfluxDbStack',
      vpcStack.vpc,
      ecsCluster.ecsCluster,
      taskExecRoleStack.role,
      albStack.influxDbAlbListener
    );

    const ingestStack = new IngestStack(
      this,
      'IngestStack',
      ecsCluster.ecsCluster,
      taskExecRoleStack.role,
      kinesisStack.writeUpstreamPerms
    );
    ingestStack.addDependency(kinesisStack);

    const ripplesStack = new RipplesStack(
      this,
      'ripplesStack',
      ecsCluster.ecsCluster,
      taskExecRoleStack.role,
      kinesisStack.readUpstreamPerms,
      kinesisStack.writeDownstreamPerms
    );
    ripplesStack.addDependency(kinesisStack);
    ripplesStack.addDependency(influxStack);

    const backendStack = new BackendStack(
      this,
      'BackendStack',
      ecsCluster.ecsCluster,
      taskExecRoleStack.role,
      albStack.backendAlbListener
    );
    backendStack.addDependency(wsApigatewayStack);
    backendStack.addDependency(influxStack);

    const dashboardStack = new DashboardStack(
      this,
      'DashboardStack',
      ecsCluster.ecsCluster,
      taskExecRoleStack.role,
      albStack.dashboardAlbListener
    );
    dashboardStack.addDependency(wsApigatewayStack);
    dashboardStack.addDependency(backendStack);
  }
}
