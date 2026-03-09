import * as cdk from 'aws-cdk-lib';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import * as servicediscovery from 'aws-cdk-lib/aws-servicediscovery';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export type GatherStackProps = cdk.NestedStackProps & {
  desiredCount: number;
  ecsCluster: ecs.ICluster;
  executionRole: iam.IRole;
  gatherDynamoDbTable: string;
  influxOrg: string;
  influxUrl: string;
  port: number;
  runAsOndemand: boolean;
  securityGroup: ec2.ISecurityGroup;
  springProfile: string;
};

export class GatherStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: GatherStackProps) {
    super(scope, id, props);

    const {
      desiredCount,
      ecsCluster,
      executionRole,
      gatherDynamoDbTable,
      influxOrg,
      influxUrl,
      port,
      runAsOndemand = false,
      securityGroup,
      springProfile
    } = props;

    securityGroup.connections.allowToAnyIpv4(ec2.Port.tcp(443), 'Allow outgoing into HTTPS only');

    const taskRole = new iam.Role(this, 'GatherTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });
    taskRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'));

    [
      { id: 'AlpacaSecret', name: 'prod/alpaca/api' },
      { id: 'DatajockeySecret', name: 'prod/datajockey/api' },
      { id: 'FredSecret', name: 'prod/fred/api' },
      { id: 'GatherSharedSecret', name: 'prod/internal/shared-secret' }
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

    const ecrRepository = ecr.Repository.fromRepositoryName(this, 'EcrRepository', 'stream-lines-gather');

    const logGroup = new logs.LogGroup(this, 'GatherLogGroup', {
      removalPolicy: RemovalPolicy.DESTROY,
      retention: logs.RetentionDays.ONE_WEEK
    });
    const logging = ecs.LogDrivers.awsLogs({
      logGroup: logGroup,
      streamPrefix: 'gather'
    });

    taskDefinition.addContainer('GatherContainer', {
      cpu: 512,
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      environment: {
        GATHER_DYNAMODB_TABLE_NAME: gatherDynamoDbTable,
        INFLUXDB_ORG: influxOrg,
        INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL: `${process.env.INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL}`,
        INFLUXDB_TOKEN_HISTORICAL_WRITE: `${process.env.INFLUXDB_TOKEN_HISTORICAL_READ_WRITE}`,
        INFLUXDB_URL: influxUrl,
        SPRING_PROFILES_ACTIVE: springProfile
      },
      logging,
      memoryLimitMiB: 1024,
      portMappings: [{ protocol: ecs.Protocol.TCP, containerPort: port }]
    });

    new ecs.FargateService(this, 'GatherEcsService', {
      assignPublicIp: false,
      capacityProviderStrategies: runAsOndemand
        ? [{ capacityProvider: 'FARGATE', weight: 1 }]
        : [{ capacityProvider: 'FARGATE_SPOT', weight: 1 }],
      cloudMapOptions: {
        name: 'gather',
        dnsTtl: cdk.Duration.seconds(30),
        dnsRecordType: servicediscovery.DnsRecordType.A
      },
      cluster: ecsCluster,
      desiredCount,
      enableExecuteCommand: true,
      securityGroups: [securityGroup],
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }
    });
  }
}
