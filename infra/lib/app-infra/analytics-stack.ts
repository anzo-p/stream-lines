import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export class AnalyticsStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    ecsCluster: ecs.Cluster,
    executionRole: iam.Role,
    readKinesisUpstreamPerms: iam.PolicyStatement,
    writeKinesisDownStreamPerms: iam.PolicyStatement,
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    const securityGroup = new ec2.SecurityGroup(
      this,
      'AnalyticsSecurityGroup',
      {
        vpc: ecsCluster.vpc,
        allowAllOutbound: true
      }
    );

    const taskRole = new iam.Role(this, 'TaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });

    taskRole.addToPolicy(readKinesisUpstreamPerms);
    taskRole.addToPolicy(writeKinesisDownStreamPerms);

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

    const taskDefinition = new ecs.FargateTaskDefinition(
      this,
      'AnalyticsTaskDefinition',
      {
        family: 'AnalyticsTaskDefinition',
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
      'stream-lines-compute'
    );

    taskDefinition.addContainer('AnalyticsContainer', {
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      memoryLimitMiB: 1024,
      cpu: 512,
      environment: {
        CHECKPOINT_PATH: `${process.env.FLINK_CHECKPOINTS_PATH},`,
        INFLUXDB_ORG: `${process.env.INFLUXDB_INIT_ORG}`,
        INFLUXDB_BUCKET: `${process.env.INFLUXDB_INIT_BUCKET}`,
        INFLUXDB_WRITE_TOKEN: `${process.env.INFLUXDB_WRITE_TOKEN}`,
        INFLUXDB_URL: `${process.env.INFLUXDB_URL}`,
        KINESIS_DOWNSTREAM_NAME: `${process.env.KINESIS_RESULTS_DOWNSTREAM}`,
        KINESIS_UPSTREAM_NAME: `${process.env.KINESIS_MARKET_DATA_UPSTREAM}`
      },
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'analytics' })
    });

    new ecs.FargateService(this, 'AnalyticsEcsService', {
      cluster: ecsCluster,
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      securityGroups: [securityGroup],
      desiredCount: 1,
      assignPublicIp: true
    });
  }
}
