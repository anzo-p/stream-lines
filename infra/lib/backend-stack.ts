import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export class BackendStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    ecsCluster: ecs.Cluster,
    executionRole: iam.Role,
    backendAlbListener: elbv2.ApplicationListener,
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    const securityGroup = new ec2.SecurityGroup(this, 'BackendSecurityGroup', {
      vpc: ecsCluster.vpc,
      allowAllOutbound: true
    });

    const taskDefinition = new ecs.FargateTaskDefinition(
      this,
      'BackendTaskDefinition',
      {
        family: 'BackendTaskDefinition',
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
      'control-tower-backend'
    );

    const containerPort = process.env.BACKEND_SERVER_PORT!;

    taskDefinition.addContainer('BackendContainer', {
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      portMappings: [
        { protocol: ecs.Protocol.TCP, containerPort: parseInt(containerPort) }
      ],
      memoryLimitMiB: 512,
      cpu: 256,
      environment: {
        GRAPHQL_SERVER_ADDRESS: `${process.env.BACKEND_SERVER_ADDRESS}`,
        GRAPHQL_SERVER_PORT: containerPort,
        INFLUXDB_ORG: `${process.env.INFLUXDB_INIT_ORG}`,
        INFLUXDB_BUCKET: `${process.env.INFLUXDB_INIT_BUCKET}`,
        INFLUXDB_READ_TOKEN: `${process.env.INFLUXDB_READ_TOKEN}`,
        INFLUXDB_URL: `${process.env.INFLUXDB_URL}`
      },
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'backend' })
    });

    const backendService = new ecs.FargateService(this, 'BackendEcsService', {
      cluster: ecsCluster,
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      securityGroups: [securityGroup],
      desiredCount: 1,
      assignPublicIp: true
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
