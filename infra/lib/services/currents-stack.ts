import * as cdk from 'aws-cdk-lib';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export class CurrentsStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    ecsCluster: ecs.Cluster,
    executionRole: iam.Role,
    securityGroup: ec2.SecurityGroup,
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    const taskRole = new iam.Role(this, 'CurrentsTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });

    taskRole.addToPolicy(
      new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
          's3:DeleteObject',
          's3:GetObject',
          's3:ListBucket',
          's3:PutObject'
        ],
        resources: [
          `arn:aws:s3:::${process.env.S3_APP_BUCKET}`,
          `arn:aws:s3:::${process.env.S3_APP_BUCKET}/*`
        ]
      })
    );

    const table = dynamodb.Table.fromTableName(
      this,
      'currents-table',
      `${process.env.CURRENTS_DYNAMODB_TABLE_NAME}`
    );

    table.grantReadWriteData(taskRole);

    const taskDefinition = new ecs.FargateTaskDefinition(
      this,
      'CurrentsTaskDefinition',
      {
        family: 'CurrentsTaskDefinition',
        executionRole,
        taskRole,
        runtimePlatform: {
          operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
          cpuArchitecture: ecs.CpuArchitecture.ARM64
        },
        memoryLimitMiB: 1024,
        cpu: 512
      }
    );

    const ecrRepository = ecr.Repository.fromRepositoryName(
      this,
      'EcrRepository',
      'stream-lines-currents'
    );

    const logGroup = new logs.LogGroup(this, 'CurrentsLogGroup', {
      retention: logs.RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY,
    });

    const logging = ecs.LogDrivers.awsLogs({
      streamPrefix: 'currents',
      logGroup: logGroup
    });

    taskDefinition.addContainer('CurrentsContainer', {
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      memoryLimitMiB: 1024,
      cpu: 512,
      environment: {
        CHECKPOINT_PATH: `${process.env.FLINK_CHECKPOINTS_CURRENTS},`,
        CURRENTS_DYNAMODB_TABLE_NAME: `${process.env.CURRENTS_DYNAMODB_TABLE_NAME}`,
        INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL: `${process.env.INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL}`,
        INFLUXDB_TOKEN_HISTORICAL_READ: `${process.env.INFLUXDB_TOKEN_HISTORICAL_READ}`,
        INFLUXDB_TOKEN_HISTORICAL_WRITE: `${process.env.INFLUXDB_TOKEN_HISTORICAL_WRITE}`,
        INFLUXDB_CONSUME_MEASURE: `${process.env.CURRENTS_INFLUXDB_SOURCE_MEASURE}`,
        INFLUXDB_ORG: `${process.env.INFLUXDB_INIT_ORG}`,
        INFLUXDB_URL: `${process.env.INFLUXDB_URL}`,
        JAVA_TOOL_OPTIONS: [
          '--add-opens=java.base/java.lang=ALL-UNNAMED',
          '--add-opens=java.base/java.math=ALL-UNNAMED',
          '--add-opens=java.base/java.time=ALL-UNNAMED',
          '--add-opens=java.base/java.util=ALL-UNNAMED',
        ].join(' ')
      },
      logging
    });

    new ecs.FargateService(this, 'CCurrentsEcsService', {
      cluster: ecsCluster,
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      securityGroups: [securityGroup],
      desiredCount: 1,
      assignPublicIp: false,
      capacityProviderStrategies: [
        {
          capacityProvider: 'FARGATE_SPOT',
          weight: 1,
        },
      ],
    });
  }
}
