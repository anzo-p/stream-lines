import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export class BackendStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    ecsCluster: ecs.Cluster,
    executionRole: iam.Role,
    securityGroup: ec2.SecurityGroup,
    backendAlbListener: elbv2.ApplicationListener,
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    const taskDefinition = new ecs.FargateTaskDefinition(this, 'BackendTaskDefinition', {
      family: 'BackendTaskDefinition',
      executionRole,
      runtimePlatform: {
        operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
        cpuArchitecture: ecs.CpuArchitecture.X86_64
      },
      memoryLimitMiB: 512,
      cpu: 256
    });

    const ecrRepository = ecr.Repository.fromRepositoryName(this, 'EcrRepository', 'stream-lines-backend');

    const logGroup = new logs.LogGroup(this, 'BackendLogGroup', {
      retention: logs.RetentionDays.ONE_WEEK,
      removalPolicy: RemovalPolicy.DESTROY
    });

    const logging = ecs.LogDrivers.awsLogs({
      streamPrefix: 'backend',
      logGroup: logGroup
    });

    const containerPort = process.env.BACKEND_SERVER_PORT!;

    taskDefinition.addContainer('BackendContainer', {
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      portMappings: [{ protocol: ecs.Protocol.TCP, containerPort: parseInt(containerPort) }],
      memoryLimitMiB: 512,
      cpu: 256,
      environment: {
        GRAPHQL_SERVER_ADDRESS: `${process.env.BACKEND_SERVER_ADDRESS}`,
        GRAPHQL_SERVER_PORT: containerPort,
        INFLUXDB_ORG: `${process.env.INFLUXDB_INIT_ORG}`,
        INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL: `${process.env.INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL}`,
        INFLUXDB_TOKEN_HISTORICAL_READ: `${process.env.INFLUXDB_TOKEN_HISTORICAL_READ}`,
        INFLUXDB_URL: `${process.env.INFLUXDB_URL}`
      },
      logging
    });

    const backendService = new ecs.FargateService(this, 'BackendEcsService', {
      cluster: ecsCluster,
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED }, // PRIVATE_WITH_EGRESS
      securityGroups: [securityGroup],
      desiredCount: 1,
      assignPublicIp: false
    });

    backendService.registerLoadBalancerTargets({
      containerName: 'BackendContainer',
      containerPort: 3030,
      newTargetGroupId: 'BackendTargetGroup',
      listener: ecs.ListenerConfig.applicationListener(backendAlbListener, {
        protocol: elbv2.ApplicationProtocol.HTTP,
        healthCheck: {
          path: '/health',
          interval: cdk.Duration.seconds(30),
          timeout: cdk.Duration.seconds(15),
          healthyThresholdCount: 3,
          unhealthyThresholdCount: 3,
          healthyHttpCodes: '200'
        }
      })
    });
  }
}
