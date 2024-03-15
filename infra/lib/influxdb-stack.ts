import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as efs from 'aws-cdk-lib/aws-efs';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export class InfluxDbStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    vpc: ec2.Vpc,
    ecsCluster: ecs.Cluster,
    executionRole: iam.Role,
    influxDbAlbListener: elbv2.ApplicationListener,
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    /*
    // this would be the way to create and grant access to an efs if you create it now
    // for databases not advisable though, as data must retain, ie. we expect much data to exists already
    const influxDbFileSystem = new efs.FileSystem(
      this,
      'StreamLinesInfluxDbFileSystem',
      {
        vpc,
        vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED }
      }
    );

    new efs.AccessPoint(this, 'InfluxDbAccessPoint', {
      fileSystem: influxDbFileSystem,
      path: '/var/lib/influxdb2',
      posixUser: {
        uid: '1000',
        gid: '1000'
      }
    });

    // connections.allowFrom when creating an EFS now
    influxDbFileSystem.connections.allowFrom(
      influxdbService,
      ec2.Port.tcp(2049),
      'Allow access to EFS on port 2049'
    );
    */

    const influxDbFileSystemSecurityGroup = new ec2.SecurityGroup(
      this,
      'StreamLinesInfluxDbEfsSecurityGroup',
      {
        vpc,
        allowAllOutbound: true
      }
    );

    const fileSystemId = process.env.INFLUXDB_FILE_SYSTEM_ID!;

    const influxDbFileSystem = efs.FileSystem.fromFileSystemAttributes(
      this,
      'StreamLinesInfluxDbDataFileSystem',
      {
        fileSystemId,
        securityGroup: ec2.SecurityGroup.fromSecurityGroupId(
          this,
          'InfluxDbFileSystemSecurityGroup',
          influxDbFileSystemSecurityGroup.securityGroupId
        )
      }
    );

    vpc.availabilityZones.forEach((_, index) => {
      new efs.CfnMountTarget(this, `EfsMountTarget${index}`, {
        fileSystemId: influxDbFileSystem.fileSystemId,
        subnetId: vpc.isolatedSubnets[index].subnetId,
        securityGroups: [influxDbFileSystemSecurityGroup.securityGroupId]
      });
    });

    const taskDefinition = new ecs.FargateTaskDefinition(
      this,
      'InfluxDbTaskDefinition',
      {
        family: 'InfluxDbTaskDefinition',
        executionRole,
        runtimePlatform: {
          operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
          cpuArchitecture: ecs.CpuArchitecture.ARM64
        },
        memoryLimitMiB: 1024,
        cpu: 512,
        volumes: [
          {
            name: 'InfluxDbDataVolume',
            efsVolumeConfiguration: {
              fileSystemId,
              authorizationConfig: {
                iam: 'ENABLED'
              },
              transitEncryption: 'ENABLED'
            }
          }
        ]
      }
    );

    influxDbFileSystem.grant(
      taskDefinition.taskRole,
      'elasticfilesystem:ClientRootAccess',
      'elasticfilesystem:ClientMount',
      'elasticfilesystem:ClientWrite'
    );

    const ecrRepository = ecr.Repository.fromRepositoryName(
      this,
      'StreamLinesEcrRepository',
      'stream-lines-influxdb'
    );

    const containerPort = parseInt(process.env.INFLUX_SERVER_PORT!);

    const influxDbContainer = taskDefinition.addContainer('InfluxDbContainer', {
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      portMappings: [{ protocol: ecs.Protocol.TCP, containerPort }],
      memoryLimitMiB: 1024,
      cpu: 512,
      environment: {
        DOCKER_INFLUXDB_INIT_MODE: `${process.env.INFLUXDB_INIT_MODE}`,
        DOCKER_INFLUXDB_INIT_USERNAME: `${process.env.INFLUXDB_INIT_USERNAME}`,
        DOCKER_INFLUXDB_INIT_PASSWORD: `${process.env.INFLUXDB_INIT_PASSWORD}`,
        DOCKER_INFLUXDB_INIT_ORG: `${process.env.INFLUXDB_INIT_ORG}`,
        DOCKER_INFLUXDB_INIT_BUCKET: `${process.env.INFLUXDB_INIT_BUCKET}`,
        DOCKER_INFLUXDB_INIT_RETENTION: `${process.env.INFLUXDB_INIT_RETENTION}`,
        DOCKER_INFLUXDB_INIT_ADMIN_TOKEN: `${process.env.INFLUXDB_INIT_ADMIN_TOKEN}`
      },
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'influxdb' })
    });

    influxDbContainer.addMountPoints({
      containerPath: '/var/lib/influxdb2',
      readOnly: false,
      sourceVolume: 'InfluxDbDataVolume'
    });

    const influxdbService = new ecs.FargateService(this, 'InfluxDbEcsService', {
      cluster: ecsCluster,
      taskDefinition,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      desiredCount: 1,
      assignPublicIp: true
    });

    influxdbService.registerLoadBalancerTargets({
      containerName: 'InfluxDbContainer',
      containerPort,
      newTargetGroupId: 'InfluxDbTargetGroup',
      listener: ecs.ListenerConfig.applicationListener(influxDbAlbListener, {
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

    influxDbFileSystemSecurityGroup.connections.allowFrom(
      influxdbService,
      ec2.Port.tcp(2049),
      'Allow access to EFS on port 2049'
    );
  }
}
