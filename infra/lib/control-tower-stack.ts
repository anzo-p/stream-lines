import * as cdk from "aws-cdk-lib";
import { Construct } from "constructs";

import { WebSocketApiGatewaySubStack } from "./api-gateway-stack";
import { KinesisStreamsSubStack } from "./kinesis-stack";

export class ControlTowerStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const webSocketApiGateway = new WebSocketApiGatewaySubStack(
      this,
      "WebSocketApiGatewaySubStack"
    );

    new KinesisStreamsSubStack(
      this,
      "KinesisStreamsSubStack",
      webSocketApiGateway.webSocketApiGatewayStageProdArn,
      webSocketApiGateway.webSocketApiGatewayConnectionsUrl
    );
  }
}
