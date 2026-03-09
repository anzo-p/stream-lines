import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as kinesis from 'aws-cdk-lib/aws-kinesis';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as s3 from 'aws-cdk-lib/aws-s3';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export type RipplesStackProps = cdk.StackProps & {
  desiredCount: number;
  ecsCluster: ecs.ICluster;
  executionRole: iam.IRole;
  flinkBucketName: string;
  influxBucket: string;
  influxOrg: string;
  influxUrl: string;
  kinesisMarketDataUpstream: kinesis.IStream;
  kinesisResultsDownStream: kinesis.IStream;
  runAsOndemand?: boolean;
  securityGroup: ec2.ISecurityGroup;
};

export class RipplesStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: RipplesStackProps) {
    super(scope, id, props);

    const {
      desiredCount,
      ecsCluster,
      executionRole,
      flinkBucketName,
      influxBucket,
      influxOrg,
      influxUrl,
      kinesisMarketDataUpstream,
      kinesisResultsDownStream,
      runAsOndemand = false,
      securityGroup
    } = props;

    const taskRole = new iam.Role(this, 'RipplesTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });

    const ripplesBucket = s3.Bucket.fromBucketName(this, 'RipplesFlinkBucket', flinkBucketName);
    ripplesBucket.grantReadWrite(taskRole);

    kinesisMarketDataUpstream.grantRead(taskRole);
    kinesisResultsDownStream.grantReadWrite(taskRole);

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'RipplesTaskDefinition', {
      cpu: 512,
      executionRole,
      family: 'RipplesTaskDefinition',
      memoryLimitMiB: 1024,
      runtimePlatform: {
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
        cpuArchitecture: ecs.CpuArchitecture.ARM64
      },
      taskRole
    });

    const ecrRepository = ecr.Repository.fromRepositoryName(this, 'EcrRepository', 'stream-lines-ripples');

    const logGroup = new logs.LogGroup(this, 'RipplesLogGroup', {
      removalPolicy: RemovalPolicy.DESTROY,
      retention: logs.RetentionDays.ONE_WEEK
    });
    const logging = ecs.LogDrivers.awsLogs({
      logGroup: logGroup,
      streamPrefix: 'ripples'
    });

    taskDefinition.addContainer('RipplesContainer', {
      cpu: 512,
      environment: {
        CHECKPOINT_PATH: `s3://${flinkBucketName}/checkpoints`,
        INFLUXDB_BUCKET_MARKET_DATA_REALTIME: influxBucket,
        INFLUXDB_ORG: influxOrg,
        INFLUXDB_TOKEN_REALTIME_WRITE: `${process.env.INFLUXDB_TOKEN_REALTIME_WRITE}`,
        INFLUXDB_URL: influxUrl,
        KINESIS_DOWNSTREAM_NAME: kinesisResultsDownStream.streamName,
        KINESIS_UPSTREAM_NAME: kinesisMarketDataUpstream.streamName,
        JAVA_TOOL_OPTIONS: [
          '--add-opens=java.base/java.lang=ALL-UNNAMED',
          '--add-opens=java.base/java.math=ALL-UNNAMED',
          '--add-opens=java.base/java.time=ALL-UNNAMED',
          '--add-opens=java.base/java.util=ALL-UNNAMED'
        ].join(' ')
      },
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      logging,
      memoryLimitMiB: 1024
    });

    new ecs.FargateService(this, 'RipplesEcsService', {
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
