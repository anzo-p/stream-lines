import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as events from 'aws-cdk-lib/aws-events';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as servicediscovery from 'aws-cdk-lib/aws-servicediscovery';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export type NarwhalStackProps = cdk.NestedStackProps & {
  desiredCount: number;
  ecsCluster: ecs.Cluster;
  executionRole: iam.Role;
  runAsOndemand: boolean;
  serviceSecurityGroup: ec2.SecurityGroup;
};

export class NarwhalStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: NarwhalStackProps) {
    super(scope, id, props);

    const taskRole = new iam.Role(this, 'NarwhalTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });
    taskRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'));

    const { desiredCount, ecsCluster, executionRole, runAsOndemand = false, serviceSecurityGroup } = props;

    const ecrRepository = ecr.Repository.fromRepositoryName(this, 'EcrRepository', 'stream-lines-narwhal');

    const baseTaskProps: ecs.FargateTaskDefinitionProps = {
      cpu: 256,
      executionRole,
      memoryLimitMiB: 512,
      runtimePlatform: {
        cpuArchitecture: ecs.CpuArchitecture.ARM64,
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX
      },
      taskRole
    };

    const baseEnvironment = {
      INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL: `${process.env.INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL}`,
      INFLUXDB_BUCKET_MARKET_DATA_REALTIME: `${process.env.INFLUXDB_BUCKET_MARKET_DATA_REALTIME}`,
      INFLUXDB_BUCKET_TRAINING_DATA: `${process.env.INFLUXDB_BUCKET_TRAINING_DATA}`,
      INFLUXDB_ORG: `${process.env.INFLUXDB_INIT_ORG}`,
      INFLUXDB_TOKEN_HISTORICAL_READ: `${process.env.INFLUXDB_TOKEN_HISTORICAL_READ}`,
      INFLUXDB_TOKEN_REALTIME_READ: `${process.env.INFLUXDB_TOKEN_REALTIME_READ}`,
      INFLUXDB_TOKEN_TRAINING_DATA_READ_WRITE: `${process.env.INFLUXDB_TOKEN_TRAINING_DATA_READ_WRITE}`,
      INFLUXDB_URL: `${process.env.INFLUXDB_URL}`,
      S3_DATA_BUCKET: `${process.env.S3_APP_BUCKET}`,
      S3_MODEL_PREFIX: `${process.env.NARWHAL_MODELS_PREFIX}`,
      S3_PREDICTION_PREFIX: `${process.env.NARWHAL_PREDICTION_DATA_PREFIX}`,
      S3_TRAINING_PREFIX: `${process.env.NARWHAL_TRAINING_DATA_PREFIX}`
    };

    const baseContainerProps: ecs.ContainerDefinitionOptions = {
      cpu: 256,
      environment: baseEnvironment,
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      memoryLimitMiB: 512
    };

    const dataBucket = s3.Bucket.fromBucketName(this, 'DataBucket', 'anzop-stream-lines');
    [
      `${process.env.NARWHAL_MODELS_PREFIX}/*`,
      `${process.env.NARWHAL_PREDICTION_DATA_PREFIX}/*`,
      `${process.env.NARWHAL_TRAINING_DATA_PREFIX}/*`
    ].forEach((path) => {
      dataBucket.grantReadWrite(taskRole, path);
    });

    // Narwhal service

    const narwhalPort = 8000;

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'NarwhalTaskDefinition', {
      ...baseTaskProps,
      family: 'NarwhalTaskDefinition'
    });

    const logGroup = new logs.LogGroup(this, 'NarwhalLogGroup', {
      removalPolicy: RemovalPolicy.DESTROY,
      retention: logs.RetentionDays.ONE_WEEK
    });

    const logging = ecs.LogDrivers.awsLogs({
      logGroup: logGroup,
      streamPrefix: 'narwhal'
    });

    taskDefinition.addContainer('NarwhalContainer', {
      ...baseContainerProps,
      logging,
      portMappings: [{ containerPort: narwhalPort, protocol: ecs.Protocol.TCP }]
    });

    const discoveryName = 'narwhal';

    new ecs.FargateService(this, 'NarwhalEcsService', {
      assignPublicIp: false,
      capacityProviderStrategies: runAsOndemand
        ? [{ capacityProvider: 'FARGATE', weight: 1 }]
        : [{ capacityProvider: 'FARGATE_SPOT', weight: 1 }],
      cloudMapOptions: {
        name: discoveryName,
        dnsTtl: cdk.Duration.seconds(30),
        dnsRecordType: servicediscovery.DnsRecordType.A
      },
      cluster: ecsCluster,
      desiredCount,
      enableExecuteCommand: true,
      securityGroups: [serviceSecurityGroup],
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }
    });

    // -- Narwhal Scheduled jobs - intention is to eventually drop that services and run only as ECS Tasks

    const schedulerTaskDefinition = new ecs.FargateTaskDefinition(this, 'NarwhalSchedulerTaskDefinition', {
      ...baseTaskProps,
      family: 'NarwhalSchedulerTaskDefinition'
    });

    const schedulerLogGroup = new logs.LogGroup(this, 'NarwhalSchedulerLogGroup', {
      removalPolicy: RemovalPolicy.DESTROY,
      retention: logs.RetentionDays.ONE_WEEK
    });

    const schedulerLogging = ecs.LogDrivers.awsLogs({
      logGroup: schedulerLogGroup,
      streamPrefix: 'narwhal-scheduler'
    });

    schedulerTaskDefinition.addContainer('NarwhalSchedulerContainer', {
      ...baseContainerProps,
      command: ['python', '-m', 'narwhal.scheduler'],
      logging: schedulerLogging
    });

    const narwhalJobSchedule: Record<string, events.Schedule> = {
      weekdaily_training_job: events.Schedule.cron({ weekDay: 'MON-FRI', hour: '16', minute: '0' }),
      weekdaily_intraday_prediction_job: events.Schedule.cron({ weekDay: 'MON-FRI', hour: '16-20/2', minute: '0' })
    };

    for (const [jobName, schedule] of Object.entries(narwhalJobSchedule)) {
      const rule = new events.Rule(this, `NarwhalRule-${jobName}`, {
        ruleName: `narwhal-scheduled-job-${jobName}`,
        schedule
      });

      rule.addTarget(
        new targets.EcsTask({
          containerOverrides: [
            {
              containerName: 'NarwhalSchedulerContainer',
              environment: [{ name: 'SCHEDULED_JOB_NAME', value: jobName }]
            }
          ],
          cluster: ecsCluster,
          securityGroups: [serviceSecurityGroup],
          subnetSelection: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
          taskDefinition: schedulerTaskDefinition
        })
      );
    }
  }
}
