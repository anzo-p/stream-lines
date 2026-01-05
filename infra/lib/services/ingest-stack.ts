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
  ecsCluster: ecs.Cluster;
  executionRole: iam.Role;
  securityGroup: ec2.SecurityGroup;
  writeKinesisUpstreamPerms: iam.PolicyStatement;
};

export class IngestStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: IngestStackProps) {
    super(scope, id, props);

    const { ecsCluster, executionRole, securityGroup, writeKinesisUpstreamPerms } = props;

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
      family: 'IngestTaskDefinition',
      executionRole,
      taskRole,
      runtimePlatform: {
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
        cpuArchitecture: ecs.CpuArchitecture.X86_64
      },
      memoryLimitMiB: 512,
      cpu: 256
    });

    const ecrRepository = ecr.Repository.fromRepositoryName(this, 'EcrRepository', 'stream-lines-ingest');

    const logGroup = new logs.LogGroup(this, 'IngestLogGroup', {
      retention: logs.RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY
    });

    const logging = ecs.LogDrivers.awsLogs({
      streamPrefix: 'ingest',
      logGroup: logGroup
    });

    taskDefinition.addContainer('IngestContainer', {
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      memoryLimitMiB: 512,
      cpu: 256,
      environment: {
        KINESIS_UPSTREAM_NAME: `${process.env.KINESIS_MARKET_DATA_UPSTREAM}`,
        MAX_WS_READS_PER_SEC: `${process.env.INGEST_MAX_WS_READS_PER_SEC}`,
        MAX_TICKER_COUNT: `${process.env.INGEST_MAX_TICKER_COUNT}`,
        TICKERS_OVERRIDE: `${process.env.INGEST_TICKERS_OVERRIDE}`,
        TOP_TICKERS_API: `${process.env.INGEST_TOP_TICKERS_API}`
      },
      logging
    });

    new ecs.FargateService(this, 'IngestEcsService', {
      cluster: ecsCluster,
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      securityGroups: [securityGroup],
      desiredCount: 1,
      assignPublicIp: false
    });
  }
}
