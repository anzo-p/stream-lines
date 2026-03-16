import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as scheduler from 'aws-cdk-lib/aws-scheduler';
import * as schedulerTargets from 'aws-cdk-lib/aws-scheduler-targets';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as s3 from 'aws-cdk-lib/aws-s3';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export type NarwhalStackProps = cdk.NestedStackProps & {
  appBucket: s3.IBucket;
  drawdownModelsLatestDirname: string;
  drawdownModelsRunsDirname: string;
  drawdownTrainingDataDirname: string;
  ecsCluster: ecs.ICluster;
  executionRole: iam.IRole;
  influxBucketMarketDataHistorical: string;
  influxBucketMarketDataRealtime: string;
  influxBucketTrainingData: string;
  influxOrg: string;
  influxUrl: string;
  serviceSecurityGroup: ec2.ISecurityGroup;
};

const narwhalJobSchedule: Record<string, scheduler.ScheduleExpression> = {
  weekdaily_training_job: scheduler.ScheduleExpression.cron({
    weekDay: 'MON-FRI',
    hour: '10',
    minute: '30',
    timeZone: cdk.TimeZone.AMERICA_NEW_YORK
  }),
  weekdaily_intraday_prediction_job: scheduler.ScheduleExpression.cron({
    weekDay: 'MON-FRI',
    hour: '10-16/3',
    minute: '0',
    timeZone: cdk.TimeZone.AMERICA_NEW_YORK
  })
};

export class NarwhalStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: NarwhalStackProps) {
    super(scope, id, props);

    const taskRole = new iam.Role(this, 'NarwhalTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });
    taskRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'));

    const {
      appBucket,
      drawdownModelsLatestDirname,
      drawdownModelsRunsDirname,
      drawdownTrainingDataDirname,
      ecsCluster,
      executionRole,
      influxBucketMarketDataHistorical,
      influxBucketMarketDataRealtime,
      influxBucketTrainingData,
      influxOrg,
      influxUrl,
      serviceSecurityGroup
    } = props;

    [`${drawdownModelsLatestDirname}/*`, `${drawdownModelsRunsDirname}/*`].forEach((path) => {
      appBucket.grantRead(taskRole, path);
    });
    appBucket.grantPut(taskRole, `${drawdownTrainingDataDirname}/*`);

    [
      { id: 'InfluxHistoricalRead', name: 'prod/influxdb/market-data-historical/read' },
      { id: 'InfluxRealtimeRead', name: 'prod/influxdb/market-data-realtime/read' },
      { id: 'InfluxTrainingWrite', name: 'prod/influxdb/training-data/read-write' }
    ].forEach(({ id, name }) => {
      const secret = secretsmanager.Secret.fromSecretNameV2(this, 'Narwhal' + id, name);
      secret.grantRead(taskRole);
    });

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'NarwhalTaskDefinition', {
      cpu: 256,
      executionRole,
      family: 'NarwhalTaskDefinition',
      memoryLimitMiB: 512,
      runtimePlatform: {
        cpuArchitecture: ecs.CpuArchitecture.ARM64,
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX
      },
      taskRole
    });

    const logGroup = new logs.LogGroup(this, 'NarwhalLogGroup', {
      removalPolicy: RemovalPolicy.DESTROY,
      retention: logs.RetentionDays.ONE_WEEK
    });
    const logging = ecs.LogDrivers.awsLogs({
      logGroup: logGroup,
      streamPrefix: 'narwhal'
    });

    const ecrRepository = ecr.Repository.fromRepositoryName(this, 'EcrRepository', 'stream-lines-narwhal');

    taskDefinition.addContainer('NarwhalContainer', {
      command: ['python', '-m', 'narwhal.scheduler'],
      cpu: 256,
      environment: {
        AWS_REGION: cdk.Stack.of(this).region,
        INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL: influxBucketMarketDataHistorical,
        INFLUXDB_BUCKET_MARKET_DATA_REALTIME: influxBucketMarketDataRealtime,
        INFLUXDB_BUCKET_TRAINING_DATA: influxBucketTrainingData,
        INFLUXDB_ORG: influxOrg,
        INFLUXDB_URL: influxUrl,
        S3_DATA_BUCKET: appBucket.bucketName,
        S3_MODELS_LATEST_KEY: `${drawdownModelsLatestDirname}`,
        S3_MODELS_RUNS_KEY: `${drawdownModelsRunsDirname}`,
        S3_TRAINING_KEY: `${drawdownTrainingDataDirname}`
      },
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      logging,
      memoryLimitMiB: 512
    });

    for (const [jobName, schedule] of Object.entries(narwhalJobSchedule)) {
      new scheduler.Schedule(this, `NarwhalSchedule-${jobName}`, {
        target: new schedulerTargets.EcsRunFargateTask(ecsCluster, {
          input: scheduler.ScheduleTargetInput.fromObject({
            containerOverrides: [
              {
                environment: [{ name: 'SCHEDULED_JOB_NAME', value: jobName }],
                name: 'NarwhalContainer'
              }
            ]
          }),
          securityGroups: [serviceSecurityGroup],
          taskDefinition,
          vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }
        }),
        schedule
      });
    }
  }
}
