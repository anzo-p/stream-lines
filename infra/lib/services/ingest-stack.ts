import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export type IngestStackProps = cdk.NestedStackProps & {
  desiredCount: number;
  ecsCluster: ecs.Cluster;
  executionRole: iam.Role;
  securityGroup: ec2.SecurityGroup;
  runAsOndemand?: boolean;
  writeKinesisUpstreamPerms: iam.PolicyStatement;
};

export class IngestStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: IngestStackProps) {
    super(scope, id, props);

    const {
      desiredCount,
      ecsCluster,
      executionRole,
      securityGroup,
      writeKinesisUpstreamPerms,
      runAsOndemand = false
    } = props;

    securityGroup.addEgressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(443), 'Allow HTTPS only');

    const taskRole = new iam.Role(this, 'IngestTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });

    taskRole.addToPolicy(writeKinesisUpstreamPerms);

    [
      { id: 'AlpacaSecret', name: 'prod/alpaca/api' },
      { id: 'GatherSharedSecret', name: 'prod/internal/shared-secret' }
    ].forEach(({ id, name }) => {
      const secret = secretsmanager.Secret.fromSecretNameV2(this, 'Ingest' + id, name);
      secret.grantRead(taskRole);
    });

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'IngestTaskDefinition', {
      cpu: 256,
      executionRole,
      family: 'IngestTaskDefinition',
      memoryLimitMiB: 512,
      runtimePlatform: {
        cpuArchitecture: ecs.CpuArchitecture.X86_64,
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX
      },
      taskRole
    });

    const ecrRepository = ecr.Repository.fromRepositoryName(this, 'EcrRepository', 'stream-lines-ingest');

    const logGroup = new logs.LogGroup(this, 'IngestLogGroup', {
      removalPolicy: RemovalPolicy.DESTROY,
      retention: logs.RetentionDays.ONE_WEEK
    });

    const logging = ecs.LogDrivers.awsLogs({
      logGroup: logGroup,
      streamPrefix: 'ingest'
    });

    taskDefinition.addContainer('IngestContainer', {
      cpu: 256,
      environment: {
        KINESIS_UPSTREAM_NAME: `${process.env.KINESIS_MARKET_DATA_UPSTREAM}`,
        MAX_WS_READS_PER_SEC: `${process.env.INGEST_MAX_WS_READS_PER_SEC}`,
        MAX_TICKER_COUNT: `${process.env.INGEST_MAX_TICKER_COUNT}`,
        TICKERS_OVERRIDE: `${process.env.INGEST_TICKERS_OVERRIDE}`,
        TOP_TICKERS_API: `${process.env.INGEST_TOP_TICKERS_API}`
      },
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      logging,
      memoryLimitMiB: 512
    });

    new ecs.FargateService(this, 'IngestEcsService', {
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
