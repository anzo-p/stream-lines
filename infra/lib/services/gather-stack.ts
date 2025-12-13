import * as cdk from 'aws-cdk-lib';
import * as dynamodb from 'aws-cdk-lib/aws-dynamodb';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as servicediscovery from 'aws-cdk-lib/aws-servicediscovery';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export class GatherStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    ecsCluster: ecs.Cluster,
    executionRole: iam.Role,
    securityGroup: ec2.SecurityGroup,
    connectingServiceSGs: { id: string; sg: ec2.SecurityGroup }[],
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    const containerPort = process.env.GATHER_SERVER_PORT ?? '8080';

    connectingServiceSGs.forEach(({ sg }) => {
      securityGroup.addIngressRule(
        sg,
        ec2.Port.tcp(Number(containerPort)),
        'Allow services to call Gather on its REST API'
      );
    });

    securityGroup.addEgressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(443), 'Allow HTTPS only');

    const taskRole = new iam.Role(this, 'GatherTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });

    const table = dynamodb.Table.fromTableName(this, 'gather-table', `${process.env.GATHER_DYNAMODB_TABLE_NAME}`);

    table.grantReadWriteData(taskRole);

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'GatherTaskDefinition', {
      family: 'GatherTaskDefinition',
      executionRole,
      taskRole,
      runtimePlatform: {
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
        cpuArchitecture: ecs.CpuArchitecture.ARM64
      },
      memoryLimitMiB: 1024,
      cpu: 512
    });

    const ecrRepository = ecr.Repository.fromRepositoryName(this, 'EcrRepository', 'stream-lines-gather');

    const logGroup = new logs.LogGroup(this, 'GatherLogGroup', {
      retention: logs.RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY
    });

    const logging = ecs.LogDrivers.awsLogs({
      streamPrefix: 'gather',
      logGroup: logGroup
    });

    taskDefinition.addContainer('GatherContainer', {
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      portMappings: [{ protocol: ecs.Protocol.TCP, containerPort: parseInt(containerPort) }],
      memoryLimitMiB: 1024,
      cpu: 512,
      environment: {
        ALPACA_API_KEY: `${process.env.GATHER_ALPACA_API_KEY}`,
        ALPACA_API_SECRET: `${process.env.GATHER_ALPACA_API_SECRET}`,
        DATA_JOCKEY_API_KEY: `${process.env.GATHER_DATA_JOCKEY_API_KEY}`,
        GATHER_DYNAMODB_TABLE_NAME: `${process.env.GATHER_DYNAMODB_TABLE_NAME}`,
        INFLUXDB_ORG: `${process.env.INFLUXDB_INIT_ORG}`,
        INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL: `${process.env.INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL}`,
        INFLUXDB_TOKEN_HISTORICAL_WRITE: `${process.env.INFLUXDB_TOKEN_HISTORICAL_READ_WRITE}`,
        INFLUXDB_URL: `${process.env.INFLUXDB_URL}`,
        JWT_SECRET_KEY: `${process.env.GATHER_JWT_SECRET_KEY}`,
        SPRING_PROFILES_ACTIVE: `${process.env.GATHER_SPRING_PROFILES_ACTIVE}`
      },
      logging
    });

    new ecs.FargateService(this, 'GatherEcsService', {
      cluster: ecsCluster,
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      securityGroups: [securityGroup],
      desiredCount: 1,
      assignPublicIp: false,
      capacityProviderStrategies: [
        {
          capacityProvider: 'FARGATE_SPOT',
          weight: 1
        }
      ],
      cloudMapOptions: {
        name: 'gather',
        dnsTtl: cdk.Duration.seconds(30),
        dnsRecordType: servicediscovery.DnsRecordType.A
      }
    });
  }
}
