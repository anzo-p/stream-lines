import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import { Construct } from 'constructs';
// import { AlbStack } from './alb-stack';
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

    // const albStack = new AlbStack(this, 'AlbStack', vpc);

    const taskExecRoleStack = new EcsTaskExecutionRole(
      this,
      'StreamLinesEcsTaskExecRole'
    );

    new NatGatewayStack(this, 'NatGatewayStack', vpc);

    const gatherStack = new GatherStack(
      this,
      'GatherStack',
      ecsCluster,
      taskExecRoleStack.role,
      securityGroups['gather'],
      ['ingest'].map(id => ({ id, sg: securityGroups[id] })),
    );

    const ripplesStack = new RipplesStack(
      this,
      'ripplesStack',
      ecsCluster,
      taskExecRoleStack.role,
      securityGroups['ripples'],
      kinesisStack.readUpstreamPerms,
      kinesisStack.writeDownstreamPerms
    );
    ripplesStack.addDependency(kinesisStack);

    const ingestStack = new IngestStack(
      this,
      'IngestStack',
      ecsCluster,
      taskExecRoleStack.role,
      securityGroups['ingest'],
      kinesisStack.writeUpstreamPerms
    );
    ingestStack.addDependency(kinesisStack);
    ingestStack.addDependency(gatherStack);

    new CurrentsStack(
      this,
      'CurrentsStack',
      ecsCluster,
      taskExecRoleStack.role,
      securityGroups['currents']
    );

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
  }
}
