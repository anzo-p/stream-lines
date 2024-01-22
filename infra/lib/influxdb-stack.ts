import * as cdk from "aws-cdk-lib";
import * as ecr from "aws-cdk-lib/aws-ecr";
import * as ecs from "aws-cdk-lib/aws-ecs";
import * as ec2 from "aws-cdk-lib/aws-ec2";
import * as efs from "aws-cdk-lib/aws-efs";
import * as elbv2 from "aws-cdk-lib/aws-elasticloadbalancingv2";
import * as iam from "aws-cdk-lib/aws-iam";
import { Construct } from "constructs";

export class InfluxDBStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    vpc: ec2.Vpc,
    ecsCluster: ecs.Cluster,
    influxDBAdminAlbListener: elbv2.ApplicationListener,
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    const influxContainerRepository = "control-tower-influxdb";

    const influxDBFileSystem = new efs.FileSystem(
      this,
      "ControlTowerInfluxFileSystem",
      {
        vpc,
        vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      }
    );

    new efs.AccessPoint(this, "CTEfsAccessPoint", {
      fileSystem: influxDBFileSystem,
      path: "/var/lib/influxdb2",
      posixUser: {
        uid: "1000",
        gid: "1000",
      },
    });

    const ecsTaskExecutionRole = new iam.Role(
      this,
      "ECSTaskExecutionRoleForInflux",
      {
        assumedBy: new iam.ServicePrincipal("ecs-tasks.amazonaws.com"),
        roleName: "ECSTaskExecutionRole",
        path: "/",
      }
    );

    ecsTaskExecutionRole.addToPolicy(
      new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
          "ecr:GetAuthorizationToken",
          "ecr:BatchCheckLayerAvailability",
          "ecr:GetDownloadUrlForLayer",
          "ecr:BatchGetImage",
        ],
        resources: [
          `arn:aws:ecr:${process.env.AWS_REGION}:${process.env.AWS_ACCOUNT}:repository/${influxContainerRepository}`,
        ],
      })
    );

    ecsTaskExecutionRole.addToPolicy(
      new iam.PolicyStatement({
        effect: iam.Effect.ALLOW,
        actions: [
          "logs:CreateLogGroup",
          "logs:CreateLogStream",
          "logs:PutLogEvents",
        ],
        resources: ["arn:aws:logs:*:*:*"],
      })
    );

    const taskDefinition = new ecs.FargateTaskDefinition(
      this,
      "InfluxDBTaskDefinition",
      {
        family: "InfluxDBTaskDefinition",
        executionRole: ecsTaskExecutionRole,
        runtimePlatform: {
          operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
          cpuArchitecture: ecs.CpuArchitecture.ARM64,
        },
        memoryLimitMiB: 1024,
        cpu: 512,
        volumes: [
          {
            name: "ControlTowerInfluxDBDataVolume",
            efsVolumeConfiguration: {
              fileSystemId: influxDBFileSystem.fileSystemId,
              authorizationConfig: {
                iam: "ENABLED",
              },
              transitEncryption: "ENABLED",
            },
          },
        ],
      }
    );

    influxDBFileSystem.grant(
      taskDefinition.taskRole,
      "elasticfilesystem:ClientRootAccess",
      "elasticfilesystem:ClientMount",
      "elasticfilesystem:ClientWrite"
    );

    const ecrRepository = ecr.Repository.fromRepositoryName(
      this,
      "ECRRepository",
      influxContainerRepository
    );

    const myContainr = taskDefinition.addContainer("InfluxDBContainer", {
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, "2.0"),
      portMappings: [{ protocol: ecs.Protocol.TCP, containerPort: 8086 }],
      memoryLimitMiB: 1024,
      cpu: 512,
      environment: {
        DOCKER_INFLUXDB_INIT_MODE: `${process.env.INFLUXDB_INIT_MODE}`,
        DOCKER_INFLUXDB_INIT_USERNAME: `${process.env.INFLUXDB_INIT_USERNAME}`,
        DOCKER_INFLUXDB_INIT_PASSWORD: `${process.env.INFLUXDB_INIT_PASSWORD}`,
        DOCKER_INFLUXDB_INIT_ORG: `${process.env.INFLUXDB_INIT_ORG}`,
        DOCKER_INFLUXDB_INIT_BUCKET: `${process.env.INFLUXDB_INIT_BUCKET}`,
        DOCKER_INFLUXDB_INIT_RETENTION: `${process.env.INFLUXDB_INIT_RETENTION}`,
        DOCKER_INFLUXDB_INIT_ADMIN_TOKEN: `${process.env.INFLUXDB_INIT_ADMIN_TOKEN}`,
      },
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: "influxdb" }),
      healthCheck: {
        command: [
          "CMD-SHELL",
          "curl --fail http://localhost:8086/health || exit 1",
        ],
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(10),
        retries: 3,
      },
    });

    myContainr.addMountPoints({
      containerPath: "/var/lib/influxdb2",
      readOnly: false,
      sourceVolume: "ControlTowerInfluxDBDataVolume",
    });

    const influxdbService = new ecs.FargateService(this, "InfluxDBEcsService", {
      cluster: ecsCluster,
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS },
      desiredCount: 1,
    });

    influxdbService.registerLoadBalancerTargets({
      containerName: "InfluxDBContainer",
      containerPort: 8086,
      newTargetGroupId: "InfluxDBTargetGroup",
      listener: ecs.ListenerConfig.applicationListener(
        influxDBAdminAlbListener,
        {
          protocol: elbv2.ApplicationProtocol.HTTP,
        }
      ),
    });

    influxDBFileSystem.connections.allowFrom(
      influxdbService,
      ec2.Port.tcp(2049),
      "Allow access to EFS on port 2049"
    );
  }
}
