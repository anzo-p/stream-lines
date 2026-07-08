import * as cdk from 'aws-cdk-lib';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as scheduler from 'aws-cdk-lib/aws-scheduler';
import * as schedulerTargets from 'aws-cdk-lib/aws-scheduler-targets';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

/*
  This deployment of Gather service runs as ECS Task
  - Scheduled by AWS EventBridge Scheduler instead of Spring Boot Scheduler
  - Runs on higher hardware specs
  - Still lower cost
  - Does not run HTTP server and cannot serve HTTP to Ingest
*/

export type GatherTaskStackProps = cdk.NestedStackProps & {
  ecsCluster: ecs.ICluster;
  executionRole: iam.IRole;
  gatherDynamoDbTable: string;
  influxBucketMarketDataHistorical: string;
  influxOrg: string;
  influxUrl: string;
  securityGroup: ec2.ISecurityGroup;
  springProfile: string;
};

const gatherJobSchedule: Record<string, scheduler.ScheduleExpression> = {
  gather_job: scheduler.ScheduleExpression.cron({
    weekDay: 'MON-FRI',
    hour: '10-16',
    minute: '5',
    timeZone: cdk.TimeZone.AMERICA_NEW_YORK
  })
};

export class GatherTaskStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: GatherTaskStackProps) {
    super(scope, id, props);

    const {
      ecsCluster,
      executionRole,
      gatherDynamoDbTable,
      influxBucketMarketDataHistorical,
      influxOrg,
      influxUrl,
      securityGroup,
      springProfile
    } = props;

    const taskRole = new iam.Role(this, 'GatherTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });
    taskRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'));

    [
      { id: 'AlpacaSecret', name: 'prod/alpaca/api' },
      { id: 'DatajockeySecret', name: 'prod/datajockey/api' },
      { id: 'FredSecret', name: 'prod/fred/api' },
      { id: 'GatherSharedSecret', name: 'prod/internal/shared-secret' },
      { id: 'InfluxHistoricalReadWrite', name: 'prod/influxdb/market-data-historical/read-write' }
    ].forEach(({ id, name }) => {
      const secret = secretsmanager.Secret.fromSecretNameV2(this, 'Gather' + id, name);
      secret.grantRead(taskRole);
    });

    const table = dynamodb.Table.fromTableName(this, 'GatherTable', gatherDynamoDbTable);
    table.grantReadWriteData(taskRole);

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'GatherTaskDefinition', {
      cpu: 512,
      executionRole,
      family: 'GatherTaskDefinition',
      memoryLimitMiB: 1024,
      runtimePlatform: {
        cpuArchitecture: ecs.CpuArchitecture.ARM64,
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX
      },
      taskRole
    });

    const logGroup = new logs.LogGroup(this, 'GatherLogGroup', {
      removalPolicy: RemovalPolicy.DESTROY,
      retention: logs.RetentionDays.ONE_WEEK
    });
    const logging = ecs.LogDrivers.awsLogs({
      logGroup: logGroup,
      streamPrefix: 'gather'
    });

    const ecrRepository = ecr.Repository.fromRepositoryName(this, 'EcrRepository', 'stream-lines-gather');

    taskDefinition.addContainer('GatherContainer', {
      cpu: 512,
      environment: {
        GATHER_DYNAMODB_TABLE_NAME: gatherDynamoDbTable,
        INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL: influxBucketMarketDataHistorical,
        INFLUXDB_ORG: influxOrg,
        INFLUXDB_URL: influxUrl,
        SPRING_MAIN_WEB_APPLICATION_TYPE: 'none',
        SPRING_PROFILES_ACTIVE: `${springProfile},batch`
      },
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      logging,
      memoryLimitMiB: 1024
    });

    for (const [jobName, schedule] of Object.entries(gatherJobSchedule)) {
      new scheduler.Schedule(this, `GatherSchedule-${jobName}`, {
        target: new schedulerTargets.EcsRunFargateTask(ecsCluster, {
          input: scheduler.ScheduleTargetInput.fromObject({
            containerOverrides: [
              {
                environment: [{ name: 'GATHER_JOB_NAME', value: 'fetch_and_process_index' }],
                name: 'GatherContainer'
              }
            ]
          }),
          securityGroups: [securityGroup],
          taskDefinition,
          vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }
        }),
        schedule
      });
    }
  }
}
