import * as cdk from "aws-cdk-lib";
import { Construct } from "constructs";
import { AlbStack } from "./alb-stack";
import { EcsClusterStack } from "./ecs-cluster-stack";
import { InfluxDBStack } from "./influxdb-stack";
import { KinesisStreamsSubStack } from "./kinesis-stack";
import { VpcStack } from "./vpc-stack";
import { WebSocketApiGatewayStack } from "./api-gateway-stack";

export class ControlTowerStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const vpcStack = new VpcStack(this, "VpcStack");

    const wsApigatewayStack = new WebSocketApiGatewayStack(
      this,
      "ApiGatewayStack"
    );

    new KinesisStreamsSubStack(
      this,
      "KinesisStack",
      wsApigatewayStack.webSocketApiGatewayStageProdArn,
      wsApigatewayStack.webSocketApiGatewayConnectionsUrl
    );

    const ecsCluster = new EcsClusterStack(
      this,
      "EcsClusterStack",
      vpcStack.vpc
    );

    const albStack = new AlbStack(this, "AlbStack", vpcStack.vpc);

    new InfluxDBStack(
      this,
      "InfluxDbStack",
      vpcStack.vpc,
      ecsCluster.ecsCluster,
      albStack.influxDBAdminAlbListener
    );

    /*
      A stack to run lambda to make token requests from now running influxdb
      set this stack to depend on InfluxDBStack
      pass these tokens to applicable services later down
    */
  }
}
