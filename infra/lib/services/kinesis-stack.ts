import * as cdk from 'aws-cdk-lib';
// import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
// import * as iam from 'aws-cdk-lib/aws-iam';
import * as kinesis from 'aws-cdk-lib/aws-kinesis';
// import * as lambda from 'aws-cdk-lib/aws-lambda';
// import * as logs from 'aws-cdk-lib/aws-logs';
import * as s3 from 'aws-cdk-lib/aws-s3';
import { Construct } from 'constructs';
// import { KinesisEventSource } from 'aws-cdk-lib/aws-lambda-event-sources';

export type KinesisStreamsStackProps = cdk.NestedStackProps & {
  appBucket: s3.IBucket;
  marketDataUpstreamName: string;
  resultsDownStreamName: string;
  resultsPusherLambdaFullPath: string;
  wsApiGatewayConnectionsUrl?: string;
  wsApiGatewayStageProdArn?: string;
  wsConnsBySymbolIndex: string;
  wsConnsTableName: string;
};

export class KinesisStreamsStack extends cdk.NestedStack {
  readonly marketDataUpstream: cdk.aws_kinesis.Stream;
  readonly resultsDownStream: cdk.aws_kinesis.Stream;

  constructor(scope: Construct, id: string, props: KinesisStreamsStackProps) {
    super(scope, id, props);

    const { marketDataUpstreamName } = props;

    this.marketDataUpstream = new kinesis.Stream(this, 'MarketDataUpStream', {
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      retentionPeriod: cdk.Duration.hours(24),
      shardCount: 1,
      streamName: marketDataUpstreamName
    });

    /*
    this.resultsDownStream = new kinesis.Stream(this, 'ResultsDownStream', {
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      retentionPeriod: cdk.Duration.hours(24),
      shardCount: 1,
      streamName: resultsDownStreamName
    });

    if (wsApiGatewayConnectionsUrl && wsApiGatewayStageProdArn) {
      const roleResultsStreamPusherLambda = new iam.Role(this, 'KinesisStreamPusherRole', {
        assumedBy: new iam.ServicePrincipal('lambda.amazonaws.com'),
        managedPolicies: [iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')]
      });

      this.resultsDownStream.grantRead(roleResultsStreamPusherLambda);

      const table = dynamodb.Table.fromTableName(this, 'WsConnectionsTable', wsConnsTableName);
      table.grantWriteData(roleResultsStreamPusherLambda);

      roleResultsStreamPusherLambda.addToPolicy(
        new iam.PolicyStatement({
          actions: ['execute-api:ManageConnections'],
          resources: [wsApiGatewayStageProdArn]
        })
      );

      const logGroup = new logs.LogGroup(this, 'StreamPusherHandlerLogGroup', {
        removalPolicy: cdk.RemovalPolicy.DESTROY,
        retention: logs.RetentionDays.ONE_WEEK
      });

      const resultsStreamPusherLambda = new lambda.Function(this, 'StreamPusherLambda', {
        code: lambda.Code.fromBucket(appBucket, resultsPusherLambdaFullPath),
        environment: {
          API_GW_CONNECTIONS_URL: wsApiGatewayConnectionsUrl,
          WS_CONNS_TABLE_NAME: wsConnsTableName,
          WS_CONNS_BY_SYMBOL_INDEX: wsConnsBySymbolIndex
        },
        functionName: 'KinesisResultsStreamPusher',
        handler: 'index.handler',
        logGroup,
        runtime: lambda.Runtime.NODEJS_20_X,
        role: roleResultsStreamPusherLambda
      });

      resultsStreamPusherLambda.addEventSource(
        new KinesisEventSource(this.resultsDownStream, {
          startingPosition: lambda.StartingPosition.LATEST,
          batchSize: 50,
          maxBatchingWindow: cdk.Duration.seconds(15),
          retryAttempts: 3
        })
      );
    }
    */
  }
}
