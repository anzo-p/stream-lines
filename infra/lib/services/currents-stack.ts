import * as cdk from 'aws-cdk-lib';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as s3 from 'aws-cdk-lib/aws-s3';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export type CurrentsStackProps = cdk.NestedStackProps & {
  currentsDynamoDbTable: string;
  desiredCount: number;
  ecsCluster: ecs.ICluster;
  executionRole: iam.IRole;
  flinkBucketName: string;
  influxBucket: string;
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
      currentsDynamoDbTable,
      desiredCount,
      ecsCluster,
      executionRole,
      flinkBucketName,
      influxBucket,
      influxMeasurement,
      influxOrg,
      influxUrl,
      runAsOndemand = false,
      securityGroup
    } = props;

    const taskRole = new iam.Role(this, 'CurrentsTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });

    const currentsBucket = s3.Bucket.fromBucketName(this, 'CurrentsFlinkBucket', flinkBucketName);
    currentsBucket.grantReadWrite(taskRole);

    const table = dynamodb.Table.fromTableName(this, 'CurrentsTable', currentsDynamoDbTable);
    table.grantReadWriteData(taskRole);

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
        CURRENTS_DYNAMODB_TABLE_NAME: currentsDynamoDbTable,
        INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL: influxBucket,
        INFLUXDB_TOKEN_HISTORICAL_READ: `${process.env.INFLUXDB_TOKEN_HISTORICAL_READ}`,
        INFLUXDB_TOKEN_HISTORICAL_WRITE: `${process.env.INFLUXDB_TOKEN_HISTORICAL_WRITE}`,
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
