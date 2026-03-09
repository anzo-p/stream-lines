import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as secretsmanager from 'aws-cdk-lib/aws-secretsmanager';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export type BackendStackProps = cdk.NestedStackProps & {
  address: string;
  albListener: elbv2.ApplicationListener;
  ecsCluster: ecs.ICluster;
  executionRole: iam.IRole;
  influxBucketMarketDataHistorical: string;
  influxOrg: string;
  influxUrl: string;
  port: number;
  securityGroup: ec2.ISecurityGroup;
};

export class BackendStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: BackendStackProps) {
    super(scope, id, props);

    const {
      address,
      albListener,
      ecsCluster,
      executionRole,
      influxBucketMarketDataHistorical,
      influxOrg,
      influxUrl,
      port,
      securityGroup
    } = props;

    const taskRole = new iam.Role(this, 'BackendTaskRole', {
      assumedBy: new iam.ServicePrincipal('ecs-tasks.amazonaws.com')
    });

    const secret = secretsmanager.Secret.fromSecretNameV2(
      this,
      'BackendInfluxHistoricalRead',
      'prod/influxdb/market-data-historical/read'
    );
    secret.grantRead(taskRole);

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'BackendTaskDefinition', {
      cpu: 256,
      executionRole,
      family: 'BackendTaskDefinition',
      memoryLimitMiB: 512,
      runtimePlatform: {
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
        cpuArchitecture: ecs.CpuArchitecture.X86_64
      },
      taskRole
    });

    const ecrRepository = ecr.Repository.fromRepositoryName(this, 'EcrRepository', 'stream-lines-backend');

    const logGroup = new logs.LogGroup(this, 'BackendLogGroup', {
      removalPolicy: RemovalPolicy.DESTROY,
      retention: logs.RetentionDays.ONE_WEEK
    });
    const logging = ecs.LogDrivers.awsLogs({
      logGroup: logGroup,
      streamPrefix: 'backend'
    });

    taskDefinition.addContainer('BackendContainer', {
      cpu: 256,
      environment: {
        GRAPHQL_SERVER_ADDRESS: address,
        GRAPHQL_SERVER_PORT: port.toString(),
        INFLUXDB_ORG: influxOrg,
        INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL: influxBucketMarketDataHistorical,
        INFLUXDB_URL: influxUrl
      },
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      logging,
      memoryLimitMiB: 512,
      portMappings: [{ containerPort: port, protocol: ecs.Protocol.TCP }]
    });

    const backendService = new ecs.FargateService(this, 'BackendEcsService', {
      assignPublicIp: false,
      cluster: ecsCluster,
      desiredCount: 1,
      securityGroups: [securityGroup],
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS }
    });

    backendService.registerLoadBalancerTargets({
      containerName: 'BackendContainer',
      containerPort: port,
      listener: ecs.ListenerConfig.applicationListener(albListener, {
        healthCheck: {
          healthyHttpCodes: '200',
          healthyThresholdCount: 3,
          interval: cdk.Duration.seconds(30),
          path: '/health',
          timeout: cdk.Duration.seconds(15),
          unhealthyThresholdCount: 3
        },
        protocol: elbv2.ApplicationProtocol.HTTP
      }),
      newTargetGroupId: 'BackendTargetGroup'
    });
  }
}
