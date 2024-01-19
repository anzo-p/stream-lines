import * as cdk from "aws-cdk-lib";
import * as iam from "aws-cdk-lib/aws-iam";
import * as kinesis from "aws-cdk-lib/aws-kinesis";
import { KinesisEventSource } from "aws-cdk-lib/aws-lambda-event-sources";
import * as lambda from "aws-cdk-lib/aws-lambda";
import * as s3 from "aws-cdk-lib/aws-s3";
import { Construct } from "constructs";

export class KinesisStreamsSubStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    webSocketApiGatewayStageProdArn: string,
    webSocketApiGatewayConnectionsUrl: string,
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    new kinesis.Stream(this, "MarketDataUpStream", {
      streamName: "control-tower-market-data-upstream",
      shardCount: 2,
      retentionPeriod: cdk.Duration.hours(24),
    });

    const resultsStream = new kinesis.Stream(this, "ResultsDownStream", {
      streamName: "control-tower-results-downstream",
      shardCount: 2,
      retentionPeriod: cdk.Duration.hours(24),
    });

    const roleResultsStreamPusherLambda = new iam.Role(
      this,
      "LambdaRoleResultsStreamPusher",
      {
        assumedBy: new iam.ServicePrincipal("lambda.amazonaws.com"),
        managedPolicies: [
          iam.ManagedPolicy.fromAwsManagedPolicyName(
            "service-role/AWSLambdaBasicExecutionRole"
          ),
        ],
      }
    );

    roleResultsStreamPusherLambda.addToPolicy(
      new iam.PolicyStatement({
        actions: [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
        ],
        effect: iam.Effect.ALLOW,
        resources: ["arn:aws:logs:*:*:*"],
      })
    );

    roleResultsStreamPusherLambda.addToPolicy(
      new iam.PolicyStatement({
        actions: ["dynamodb:Query", "dynamodb:DeleteItem"],
        effect: iam.Effect.ALLOW,
        resources: [
          `arn:aws:dynamodb:${process.env.AWS_REGION}:${process.env.AWS_ACCOUNT}:table/${process.env.WS_CONNS_TABLE_NAME}`,
          `arn:aws:dynamodb:${process.env.AWS_REGION}:${process.env.AWS_ACCOUNT}:table/${process.env.WS_CONNS_TABLE_NAME}/index/${process.env.WS_CONNS_BY_SYMBOL_INDEX}`,
        ],
      })
    );

    roleResultsStreamPusherLambda.addToPolicy(
      new iam.PolicyStatement({
        actions: [
          "kinesis:DescribeStream",
          "kinesis:GetRecord",
          "kinesis:GetRecords",
          "kinesis:GetShardIterator",
          "kinesis:ListShards",
          "kinesis:SubscribeToShard",
        ],
        effect: iam.Effect.ALLOW,
        resources: [
          `arn:aws:kinesis:${process.env.AWS_REGION}:${process.env.AWS_ACCOUNT}:stream/${resultsStream.streamName}`,
        ],
      })
    );

    roleResultsStreamPusherLambda.addToPolicy(
      new iam.PolicyStatement({
        actions: ["execute-api:ManageConnections"],
        resources: [webSocketApiGatewayStageProdArn],
      })
    );

    const bucketResultStreamPusherLambda = s3.Bucket.fromBucketName(
      this,
      "LambdaCodeBucket",
      `${process.env.S3_BUCKET_LAMBDAS}`
    );

    const resultsStreamPusherLambda = new lambda.Function(
      this,
      "ResultsStreamPusher",
      {
        functionName: "KinesisResultsStreamPusher",
        runtime: lambda.Runtime.NODEJS_20_X,
        handler: "index.handler",
        code: lambda.Code.fromBucket(
          bucketResultStreamPusherLambda,
          `${process.env.S3_KEY_RESULTS_PUSHER}`
        ),
        role: roleResultsStreamPusherLambda,
        environment: {
          API_GW_CONNECTIONS_URL: `${webSocketApiGatewayConnectionsUrl}`,
          WS_CONNS_TABLE_NAME: `${process.env.WS_CONNS_TABLE_NAME}`,
          WS_CONNS_BY_SYMBOL_INDEX: `${process.env.WS_CONNS_BY_SYMBOL_INDEX}`,
        },
      }
    );

    resultsStreamPusherLambda.addEventSource(
      new KinesisEventSource(resultsStream, {
        startingPosition: lambda.StartingPosition.LATEST,
        batchSize: 50,
        maxBatchingWindow: cdk.Duration.seconds(15),
        retryAttempts: 3,
      })
    );
  }
}
