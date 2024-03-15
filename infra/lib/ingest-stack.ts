import * as cdk from 'aws-cdk-lib';
import * as cloudwatch from 'aws-cdk-lib/aws-cloudwatch';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export class IngestStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    ecsCluster: ecs.Cluster,
    executionRole: iam.Role,
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    const secirutyGroup = new ec2.SecurityGroup(this, 'IngestSecurityGroup', {
      vpc: ecsCluster.vpc,
      allowAllOutbound: true
    });

    const taskDefinition = new ecs.FargateTaskDefinition(
      this,
      'IngestTaskDefinition',
      {
        family: 'IngestTaskDefinition',
        executionRole,
        runtimePlatform: {
          operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
          cpuArchitecture: ecs.CpuArchitecture.X86_64
        },
        memoryLimitMiB: 512,
        cpu: 256
      }
    );

    const ecrRepository = ecr.Repository.fromRepositoryName(
      this,
      'EcrRepository',
      'stream-lines-ingest'
    );

    taskDefinition.addContainer('IngestContainer', {
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      memoryLimitMiB: 512,
      cpu: 256,
      environment: {
        ALPACA_API_KEY: `${process.env.INGEST_ALPACA_API_KEY}`,
        ALPACA_API_SECRET: `${process.env.INGEST_ALPACA_API_SECRET}`,
        AWS_ACCESS_KEY_ID: `${process.env.AWS_ACCESS_KEY_ID}`,
        AWS_SECRET_ACCESS_KEY: `${process.env.AWS_SECRET_ACCESS_KEY}`,
        AWS_REGION: `${process.env.AWS_REGION}`,
        KINESIS_UPSTREAM_NAME: `${process.env.KINESIS_MARKET_DATA_UPSTREAM}`,
        MAX_WS_READS_PER_SEC: `${process.env.INGEST_MAX_WS_READS_PER_SEC}`
      },
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'ingest' })
    });

    const ingestService = new ecs.FargateService(this, 'IngestEcsService', {
      cluster: ecsCluster,
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      securityGroups: [secirutyGroup],
      desiredCount: 1,
      assignPublicIp: true
    });

    /*
    const cpuUtilizationMetric = new cloudwatch.Metric({
      namespace: 'AWS/ECS',
      metricName: 'CPUUtilization',
      dimensionsMap: {
        ClusterName: ecsCluster.clusterName,
        ServiceName: ingestService.serviceName
      },
      statistic: 'SampleCount',
      period: cdk.Duration.minutes(1)
    });

    new cloudwatch.Alarm(this, 'AlarmIngestNoRunningInstances', {
      alarmName: 'AlarmIngestNoRunningInstances',
      alarmDescription: 'Alarm if no Ingest instances are running',
      metric: cpuUtilizationMetric,
      comparisonOperator: cloudwatch.ComparisonOperator.LESS_THAN_THRESHOLD,
      threshold: 1,
      evaluationPeriods: 1,
      actionsEnabled: true,
      treatMissingData: cloudwatch.TreatMissingData.BREACHING
    });
    */
  }
}
