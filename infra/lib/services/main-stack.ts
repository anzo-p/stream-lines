import * as cdk from 'aws-cdk-lib';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as s3 from 'aws-cdk-lib/aws-s3';
import { Construct } from 'constructs';
// import { AlbStack } from './alb-stack';
import { AutoTeardownStack } from './auto-teardown';
// import { BackendStack } from './backend-stack';
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
// import { WebSocketApiGatewayStack } from './api-gateway-stack';

interface ServicesStackProps extends cdk.StackProps {
  ecsCluster: ecs.Cluster;
  vpc: ec2.Vpc;
}

export class ServicesStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props: ServicesStackProps) {
    super(scope, id, props);

    const { ecsCluster, vpc } = props;

    const autoTeardownDenied = this.node.tryGetContext('autoTeardown') === 'false';
    const runOnlyOnDemandServices = this.node.tryGetContext('onlyOnDemand') === 'true';
    const runAllServicesOnDemand = this.node.tryGetContext('runAllAsOnDemand') === 'true';

    const influxSg = ec2.SecurityGroup.fromSecurityGroupId(
      this,
      'ImportedInfluxSg',
      cdk.Fn.importValue('StreamLines-Infra:InfluxSgId'),
      { mutable: true }
    );

    // const backendSg = new ec2.SecurityGroup(this, 'BackendSecurityGroup', { vpc, allowAllOutbound: true });
    const currentsSg = new ec2.SecurityGroup(this, 'CurrentsSecurityGroup', { vpc, allowAllOutbound: true });
    const gatherSg = new ec2.SecurityGroup(this, 'GatherSecurityGroup', { vpc, allowAllOutbound: true });
    const ingestSg = new ec2.SecurityGroup(this, 'IngestSecurityGroup', { vpc, allowAllOutbound: true });
    const narwhalSg = new ec2.SecurityGroup(this, 'NarwhalSecurityGroup', { vpc, allowAllOutbound: true });
    const ripplesSg = new ec2.SecurityGroup(this, 'RipplesSecurityGroup', { vpc, allowAllOutbound: true });

    [currentsSg, gatherSg, narwhalSg, ripplesSg].forEach((sg) => {
      influxSg.connections.allowFrom(
        sg,
        ec2.Port.tcp(Number(process.env.INFLUXDB_SERVER_PORT!)),
        `${sg.node.id}-to-Influx`
      );
    });

    gatherSg.connections.allowFrom(ingestSg, ec2.Port.tcp(Number(process.env.GATHER_SERVER_PORT!)), 'Ingest to Gather');

    const appBucket = s3.Bucket.fromBucketName(this, 'AppBucket', process.env.S3_APP_BUCKET!);

    /*
    const wsApigatewayStack = new WebSocketApiGatewayStack(this, 'ApiGatewayStack', {
      appBucket,
      acmApigwCertId: process.env.ACM_APIGW_CERT!,
      wsApiDomainName: process.env.WS_API_DOMAIN_NAME!,
      wsApiSubdomain: process.env.WS_API_SUBDOMAIN!,
      wsConnsHandlerLambdaFullPath: process.env.S3_KEY_WS_CONN_HANDLER!,
      wsConnsTableName: process.env.WS_CONNS_TABLE_NAME!
    });
    */

    const kinesisStack = new KinesisStreamsStack(this, 'KinesisStack', {
      appBucket,
      marketDataUpstreamName: process.env.KINESIS_MARKET_DATA_UPSTREAM!,
      resultsDownStreamName: process.env.KINESIS_RESULTS_DOWNSTREAM!,
      resultsPusherLambdaFullPath: process.env.S3_KEY_RESULTS_PUSHER!,
      // wsApiGatewayConnectionsUrl: wsApigatewayStack.wsApiGatewayConnectionsUrl,
      // wsApiGatewayStageProdArn: wsApigatewayStack.wsApiGatewayStageProdArn,
      wsConnsBySymbolIndex: process.env.WS_CONNS_BY_SYMBOL_INDEX!,
      wsConnsTableName: process.env.WS_CONNS_TABLE_NAME!
    });

    // const albStack = new AlbStack(this, 'AlbStack', vpc);

    const taskExecRoleStack = new EcsTaskExecutionRole(this, 'StreamLinesEcsTaskExecRole');

    new NatGatewayStack(this, 'NatGatewayStack', vpc);

    const executionRole = taskExecRoleStack.role;
    const influxOrg = process.env.INFLUXDB_INIT_ORG!;
    const influxUrl = process.env.INFLUXDB_URL!;
    const influxBucketMarketDataHistorical = process.env.INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL!;
    const influxBucketMarketDataRealtime = process.env.INFLUXDB_BUCKET_MARKET_DATA_REALTIME!;

    let gatherStack: GatherStack | undefined;
    if (!runOnlyOnDemandServices || runAllServicesOnDemand) {
      gatherStack = new GatherStack(this, 'GatherStack', {
        desiredCount: 1,
        ecsCluster,
        executionRole,
        gatherDynamoDbTable: process.env.GATHER_DYNAMODB_TABLE_NAME!,
        influxBucketMarketDataHistorical,
        influxOrg,
        influxUrl,
        port: Number(process.env.GATHER_SERVER_PORT!),
        runAsOndemand: runAllServicesOnDemand,
        securityGroup: gatherSg,
        springProfile: process.env.GATHER_SPRING_PROFILES_ACTIVE!
      });

      new CurrentsStack(this, 'CurrentsStack', {
        currentsDynamoDbTable: process.env.CURRENTS_DYNAMODB_TABLE_NAME!,
        desiredCount: 1,
        ecsCluster,
        executionRole,
        flinkBucketName: process.env.S3_CURRENTS_BUCKET!,
        influxBucketMarketDataHistorical,
        influxMeasurement: process.env.CURRENTS_INFLUXDB_SOURCE_MEASURE!,
        influxOrg,
        influxUrl,
        runAsOndemand: runAllServicesOnDemand,
        securityGroup: currentsSg
      });
    }

    const ripplesStack = new RipplesStack(this, 'RipplesStack', {
      desiredCount: 1,
      ecsCluster,
      executionRole,
      flinkBucketName: process.env.S3_RIPPLES_BUCKET!,
      influxBucketMarketDataRealtime,
      influxOrg,
      influxUrl,
      kinesisMarketDataUpstream: kinesisStack.marketDataUpstream,
      kinesisResultsDownStream: kinesisStack.resultsDownStream,
      runAsOndemand: true,
      securityGroup: ripplesSg
    });
    ripplesStack.addDependency(kinesisStack);

    const ingestStack = new IngestStack(this, 'IngestStack', {
      desiredCount: 1,
      ecsCluster,
      executionRole,
      kinesisMarketDataUpstream: kinesisStack.marketDataUpstream,
      maxWebsocketReadsPerSec: process.env.INGEST_MAX_WS_READS_PER_SEC!,
      maxTickerCount: process.env.INGEST_MAX_TICKER_COUNT!,
      runAsOndemand: true,
      securityGroup: ingestSg,
      tickersOverride: process.env.INGEST_TICKERS_OVERRIDE!,
      topTickersApi: process.env.INGEST_TOP_TICKERS_API!
    });
    ingestStack.addDependency(kinesisStack);
    if (gatherStack) ingestStack.addDependency(gatherStack);

    /*
    const backendStack = new BackendStack(this, 'BackendStack', {
      address: process.env.BACKEND_SERVER_ADDRESS!,
      albListener: albStack.backendAlbListener,
      ecsCluster,
      executionRole,
      influxBucketMarketDataHistorical,
      influxOrg,
      influxUrl,
      port: Number(process.env.BACKEND_SERVER_PORT!),
      securityGroup: backendSg
    });
    backendStack.addDependency(wsApigatewayStack);

    // should frontend be run out of S3 via CloudFront?
    const dashboardStack = new DashboardStack(this, 'DashboardStack', {
      dashboardAlbListener: albStack.dashboardAlbListener,
      ecsCluster,
      executionRole
    });
    dashboardStack.addDependency(wsApigatewayStack);
    dashboardStack.addDependency(backendStack);
    */

    new NarwhalStack(this, 'NarwhalStack', {
      appBucket,
      ecsCluster,
      executionRole,
      drawdownModelsLatestDirname: process.env.NARWHAL_MODELS_LATEST_PATH!,
      drawdownModelsRunsDirname: process.env.NARWHAL_MODELS_RUNS_PATH!,
      drawdownTrainingDataDirname: process.env.NARWHAL_TRAINING_DATA_PATH!,
      influxBucketMarketDataHistorical,
      influxBucketMarketDataRealtime,
      influxBucketTrainingData: process.env.INFLUXDB_BUCKET_TRAINING_DATA!,
      influxOrg,
      influxUrl,
      serviceSecurityGroup: narwhalSg
    });

    new DrawdownSagemakerStack(this, 'SagemakerStack', {
      appBucket,
      modelsLatestDirname: process.env.NARWHAL_MODELS_LATEST_PATH!,
      modelsRunsDirname: process.env.NARWHAL_MODELS_RUNS_PATH!,
      trainingDirname: process.env.NARWHAL_TRAINING_DATA_PATH!,
      xgboostImage: process.env.SAGEMAKER_XGBOOST_IMAGE!
    });

    if (!autoTeardownDenied) {
      new AutoTeardownStack(this, 'AutoTeardownStack', {
        targetStackName: cdk.Stack.of(this).stackName,
        targetStackArn: cdk.Stack.of(this).stackId
      });
    }
  }
}
