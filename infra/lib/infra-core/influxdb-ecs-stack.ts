import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';

export class InfluxDbEcsStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    ecsCluster: ecs.Cluster,
    influxDbSecurityGroup: ec2.SecurityGroup,
    connectingServiceSGs: { key: string; sg: ec2.SecurityGroup }[],
    props?: cdk.StackProps
  ) {
    super(scope, id, props);

    const influxDbPort = Number(process.env.INFLUXDB_SERVER_PORT);
    if (!influxDbPort) throw new Error('Invalid INFLUXDB_SERVER_PORT');

    connectingServiceSGs.forEach(({ key, sg }) => {
      influxDbSecurityGroup.connections.allowFrom(
        sg,
        ec2.Port.tcp(influxDbPort),
        `${key}-to-Influx`
      );
    });

    const taskDefinition = new ecs.Ec2TaskDefinition(this, 'InfluxDbTaskDefinition', {
      networkMode: ecs.NetworkMode.AWS_VPC,
      family: 'InfluxDbTaskDefinition',
    });

    taskDefinition.addVolume({
      name: 'InfluxDbDataVolume',
      host: {
        sourcePath: '/mnt/influxdb-data',
      },
    });

    const ecrRepository = ecr.Repository.fromRepositoryName(
      this,
      'StreamLinesEcrRepository',
      'stream-lines-influxdb'
    );

    const containerPort = parseInt(process.env.INFLUXDB_SERVER_PORT!);

    const influxDbContainer = taskDefinition.addContainer('InfluxDbContainer', {
      image: ecs.ContainerImage.fromEcrRepository(ecrRepository, 'latest'),
      portMappings: [{ protocol: ecs.Protocol.TCP, containerPort }],
      memoryReservationMiB: 512,
      environment: {
        DOCKER_INFLUXDB_INIT_MODE: `${process.env.INFLUXDB_INIT_MODE}`,
        DOCKER_INFLUXDB_INIT_USERNAME: `${process.env.INFLUXDB_INIT_USERNAME}`,
        DOCKER_INFLUXDB_INIT_PASSWORD: `${process.env.INFLUXDB_INIT_PASSWORD}`,
        DOCKER_INFLUXDB_INIT_ORG: `${process.env.INFLUXDB_INIT_ORG}`,
        DOCKER_INFLUXDB_INIT_BUCKET: `${process.env.INFLUXDB_INIT_BUCKET}`,
        DOCKER_INFLUXDB_INIT_RETENTION: `${process.env.INFLUXDB_INIT_RETENTION}`,
        DOCKER_INFLUXDB_INIT_ADMIN_TOKEN: `${process.env.INFLUXDB_INIT_ADMIN_TOKEN}`
      },
      logging: ecs.LogDrivers.awsLogs({ streamPrefix: 'influxdb' }),
    });

    influxDbContainer.addMountPoints({
      containerPath: '/var/lib/influxdb2',
      readOnly: false,
      sourceVolume: 'InfluxDbDataVolume',
    });

    new ecs.Ec2Service(this, 'InfluxDbEcsService', {
      assignPublicIp: false,
      cluster: ecsCluster,
      taskDefinition,
      desiredCount: 1,
      enableExecuteCommand: true,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      securityGroups: [influxDbSecurityGroup],
      cloudMapOptions: {
        name: 'influxdb',
      },
    });
  }
}
