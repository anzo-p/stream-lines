import * as cdk from "aws-cdk-lib";
import * as dynamodb from "aws-cdk-lib/aws-dynamodb";
import { Construct } from "constructs";

export class DynamodbStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const table = new dynamodb.Table(
      this,
      "ControlTowerWebsocketConnectionTable",
      {
        tableName: "ControlTowerWebsocketConnectionTable",
        partitionKey: {
          name: "connectionId",
          type: dynamodb.AttributeType.STRING,
        },
        billingMode: dynamodb.BillingMode.PAY_PER_REQUEST,
        removalPolicy: cdk.RemovalPolicy.RETAIN,
      }
    );

    table.addGlobalSecondaryIndex({
      indexName: "ControlTowerWebsocketGetConnectionsBySymbolIndex",
      partitionKey: {
        name: "symbol",
        type: dynamodb.AttributeType.STRING,
      },
      projectionType: dynamodb.ProjectionType.KEYS_ONLY,
    });
  }
}
