import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as efs from 'aws-cdk-lib/aws-efs';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export class InfluxDbStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    vpc: ec2.Vpc,
    ecsCluster: ecs.Cluster,
    executionRole: iam.Role,
    connectingServiceSGs: { key: string; sg: ec2.SecurityGroup }[],
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    const influxDbServiceSecurityGroup = new ec2.SecurityGroup(
      this,
      'InfluxDbServiceSecurityGroup',
      {
        vpc,
        allowAllOutbound: true,
      });

    const influxDbPort = Number(process.env.INFLUXDB_SERVER_PORT);
    if (!influxDbPort) throw new Error('Invalid INFLUXDB_SERVER_PORT');

    connectingServiceSGs.forEach(({ key, sg }) => {
      influxDbServiceSecurityGroup.connections.allowFrom(
        sg,
        ec2.Port.tcp(influxDbPort),
        `${key}-to-Influx`
      );
    });

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

    const containerPort = parseInt(process.env.INFLUXDB_SERVER_PORT!);

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
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      desiredCount: 1,
      assignPublicIp: false,
      securityGroups: [influxDbServiceSecurityGroup],
      cloudMapOptions: {
        name: 'influxdb', // ie. influxdb.stream-lines.local
      },
    });

    influxDbFileSystemSecurityGroup.connections.allowFrom(
      influxdbService,
      ec2.Port.tcp(2049),
      'Allow access to EFS on port 2049'
    );
  }
}
