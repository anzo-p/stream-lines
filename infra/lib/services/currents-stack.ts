import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as s3 from 'aws-cdk-lib/aws-s3';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export type CurrentsStackProps = cdk.NestedStackProps & {
  desiredCount: number;
  ecsCluster: ecs.ICluster;
  executionRole: iam.IRole;
  flinkBucketName: string;
  influxBucketMarketDataHistorical: string;
  influxMeasurement: string;
  influxOrg: string;
  influxUrl: string;
  runAsOndemand: boolean;
  securityGroup: ec2.ISecurityGroup;
};

export class CurrentsStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: CurrentsStackProps) {
    super(scope, id, props);

    const {
      desiredCount,
      ecsCluster,
      executionRole,
      flinkBucketName,
      influxBucketMarketDataHistorical,
      influxMeasurement,
      influxOrg,
      influxUrl,
      runAsOndemand = false,
      securityGroup
    } = props;

    const taskRole = new iam.Role(this, 'CurrentsTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });

    const secret = secretsmanager.Secret.fromSecretNameV2(
      this,
      'CurrentsInfluxHistoricalReadWrite',
      'prod/influxdb/market-data-historical/read-write'
    );
    secret.grantRead(taskRole);

    const currentsBucket = s3.Bucket.fromBucketName(this, 'CurrentsFlinkBucket', flinkBucketName);
    currentsBucket.grantReadWrite(taskRole);

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'CurrentsTaskDefinition', {
      cpu: 512,
      executionRole,
      family: 'CurrentsTaskDefinition',
      memoryLimitMiB: 1024,
      runtimePlatform: {
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
        cpuArchitecture: ecs.CpuArchitecture.ARM64
      },
      taskRole
    });

    const ecrRepository = ecr.Repository.fromRepositoryName(this, 'EcrRepository', 'stream-lines-currents');

    const logGroup = new logs.LogGroup(this, 'CurrentsLogGroup', {
      removalPolicy: RemovalPolicy.DESTROY,
      retention: logs.RetentionDays.ONE_WEEK
    });
    const logging = ecs.LogDrivers.awsLogs({
      logGroup: logGroup,
      streamPrefix: 'currents'
    });

    taskDefinition.addContainer('CurrentsContainer', {
      cpu: 512,
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      environment: {
        CHECKPOINT_PATH: `s3://${flinkBucketName}/checkpoints`,
        INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL: influxBucketMarketDataHistorical,
        INFLUXDB_CONSUME_MEASURE: influxMeasurement,
        INFLUXDB_ORG: influxOrg,
        INFLUXDB_URL: influxUrl,
        JAVA_TOOL_OPTIONS: [
          '--add-opens=java.base/java.lang=ALL-UNNAMED',
          '--add-opens=java.base/java.math=ALL-UNNAMED',
          '--add-opens=java.base/java.time=ALL-UNNAMED',
          '--add-opens=java.base/java.util=ALL-UNNAMED'
        ].join(' ')
      },
      logging,
      memoryLimitMiB: 1024
    });

    new ecs.FargateService(this, 'CurrentsEcsService', {
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
