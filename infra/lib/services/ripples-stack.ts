import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export type RipplesStackProps = cdk.StackProps & {
  desiredCount: number;
  ecsCluster: ecs.Cluster;
  executionRole: iam.Role;
  readKinesisUpstreamPerms: iam.PolicyStatement;
  runAsOndemand?: boolean;
  securityGroup: ec2.SecurityGroup;
  writeKinesisDownStreamPerms: iam.PolicyStatement;
};

export class RipplesStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: RipplesStackProps) {
    super(scope, id, props);

    const {
      desiredCount,
      ecsCluster,
      executionRole,
      readKinesisUpstreamPerms,
      runAsOndemand = false,
      securityGroup,
      writeKinesisDownStreamPerms
    } = props;

    const taskRole = new iam.Role(this, 'TaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });

    taskRole.addToPolicy(readKinesisUpstreamPerms);
    taskRole.addToPolicy(writeKinesisDownStreamPerms);

    taskRole.addToPolicy(
      new iam.PolicyStatement({
        actions: ['s3:DeleteObject', 's3:GetObject', 's3:ListBucket', 's3:PutObject'],
        effect: iam.Effect.ALLOW,
        resources: [`arn:aws:s3:::${process.env.S3_APP_BUCKET}`, `arn:aws:s3:::${process.env.S3_APP_BUCKET}/*`]
      })
    );

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
        CHECKPOINT_PATH: `${process.env.FLINK_CHECKPOINTS_RIPPLES},`,
        INFLUXDB_BUCKET_MARKET_DATA_REALTIME: `${process.env.INFLUXDB_BUCKET_MARKET_DATA_REALTIME}`,
        INFLUXDB_ORG: `${process.env.INFLUXDB_INIT_ORG}`,
        INFLUXDB_TOKEN_REALTIME_WRITE: `${process.env.INFLUXDB_TOKEN_REALTIME_WRITE}`,
        INFLUXDB_URL: `${process.env.INFLUXDB_URL}`,
        KINESIS_DOWNSTREAM_NAME: `${process.env.KINESIS_RESULTS_DOWNSTREAM}`,
        KINESIS_UPSTREAM_NAME: `${process.env.KINESIS_MARKET_DATA_UPSTREAM}`,
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
