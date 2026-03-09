import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as kinesis from 'aws-cdk-lib/aws-kinesis';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export type IngestStackProps = cdk.NestedStackProps & {
  desiredCount: number;
  ecsCluster: ecs.ICluster;
  executionRole: iam.IRole;
  kinesisMarketDataUpstream: kinesis.IStream;
  maxTickerCount: string;
  maxWebsocketReadsPerSec: string;
  runAsOndemand?: boolean;
  securityGroup: ec2.ISecurityGroup;
  tickersOverride: string;
  topTickersApi: string;
};

export class IngestStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: IngestStackProps) {
    super(scope, id, props);

    const {
      desiredCount,
      ecsCluster,
      executionRole,
      kinesisMarketDataUpstream,
      maxTickerCount,
      maxWebsocketReadsPerSec,
      runAsOndemand = false,
      securityGroup,
      tickersOverride,
      topTickersApi
    } = props;

    securityGroup.connections.allowToAnyIpv4(ec2.Port.tcp(443), 'Allow outgoing into HTTPS only');

    const taskRole = new iam.Role(this, 'IngestTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });

    [
      { id: 'AlpacaSecret', name: 'prod/alpaca/api' },
      { id: 'GatherSharedSecret', name: 'prod/internal/shared-secret' }
    ].forEach(({ id, name }) => {
      const secret = secretsmanager.Secret.fromSecretNameV2(this, 'Ingest' + id, name);
      secret.grantRead(taskRole);
    });

    kinesisMarketDataUpstream.grantReadWrite(taskRole);

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'IngestTaskDefinition', {
      cpu: 256,
      executionRole,
      family: 'IngestTaskDefinition',
      memoryLimitMiB: 512,
      runtimePlatform: {
        cpuArchitecture: ecs.CpuArchitecture.X86_64,
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX
      },
      taskRole
    });

    const ecrRepository = ecr.Repository.fromRepositoryName(this, 'EcrRepository', 'stream-lines-ingest');

    const logGroup = new logs.LogGroup(this, 'IngestLogGroup', {
      removalPolicy: RemovalPolicy.DESTROY,
      retention: logs.RetentionDays.ONE_WEEK
    });
    const logging = ecs.LogDrivers.awsLogs({
      logGroup: logGroup,
      streamPrefix: 'ingest'
    });

    taskDefinition.addContainer('IngestContainer', {
      cpu: 256,
      environment: {
        KINESIS_UPSTREAM_NAME: kinesisMarketDataUpstream.streamName,
        MAX_TICKER_COUNT: maxTickerCount,
        MAX_WS_READS_PER_SEC: maxWebsocketReadsPerSec,
        TICKERS_OVERRIDE: tickersOverride,
        TOP_TICKERS_API: topTickersApi
      },
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      logging,
      memoryLimitMiB: 512
    });

    new ecs.FargateService(this, 'IngestEcsService', {
      assignPublicIp: false,
      capacityProviderStrategies: runAsOndemand
        ? [{ capacityProvider: 'FARGATE', weight: 1 }]
        : [{ capacityProvider: 'FARGATE_SPOT', weight: 1 }],
      cluster: ecsCluster,
      desiredCount,
      securityGroups: [securityGroup],
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }
    });
  }
}
