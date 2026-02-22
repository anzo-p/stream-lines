import * as cloudmap from 'aws-cdk-lib/aws-servicediscovery';
import * as cdk from 'aws-cdk-lib';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as ssm from 'aws-cdk-lib/aws-ssm';
import { RemovalPolicy } from 'aws-cdk-lib';
import { Construct } from 'constructs';

export type InfluxDbStackProps = cdk.NestedStackProps & {
  ecsCluster: ecs.Cluster;
  securityGroup: ec2.SecurityGroup;
  ssmRole: iam.Role;
  vpc: ec2.Vpc;
};

export class InfluxDbStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: InfluxDbStackProps) {
    super(scope, id, props);

    const { ecsCluster, securityGroup, ssmRole, vpc } = props;

    const influxDbPort = Number(process.env.INFLUXDB_SERVER_PORT!);

    const influxDbInstance = new ec2.Instance(this, 'InfluxDbEc2Instance', {
      availabilityZone: 'eu-north-1a', // same as EBS volume
      instanceType: new ec2.InstanceType('t4g.medium'),
      keyName: process.env.KEY_NAME_INFLUXDB,
      machineImage: ec2.MachineImage.latestAmazonLinux2023({
        cpuType: ec2.AmazonLinuxCpuType.ARM_64
      }),
      role: ssmRole,
      securityGroup,
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED }
    });

    new logs.LogGroup(this, 'InfluxEc2LogGroup', {
      logGroupName: '/ec2/influxdb',
      removalPolicy: RemovalPolicy.DESTROY,
      retention: logs.RetentionDays.ONE_WEEK
    });

    new ec2.CfnVolumeAttachment(this, 'InfluxDataAttachment', {
      device: '/dev/xvdf',
      instanceId: influxDbInstance.instanceId,
      volumeId: `${process.env.INFLUXDB_FILE_SYSTEM_ID}`
    });

    const namespace = ecsCluster.defaultCloudMapNamespace;
    if (!namespace) {
      throw new Error('Cluster has no default Cloud Map namespace');
    }

    const influxDiscoveryService = new cloudmap.Service(this, 'InfluxDbDiscoveryService', {
      dnsTtl: cdk.Duration.seconds(30),
      dnsRecordType: cloudmap.DnsRecordType.A,
      name: 'influxdb',
      namespace
    });

    new cloudmap.IpInstance(this, 'InfluxDbInstance', {
      ipv4: influxDbInstance.instancePrivateIp,
      port: influxDbPort,
      service: influxDiscoveryService
    });

    // only SSM has internet acess, via Vpc Endpoint
    new ssm.CfnAssociation(this, 'InstallDockerAssociation', {
      name: 'AWS-RunShellScript',
      parameters: {
        commands: [
          [
            'sudo yum update -y',
            'sudo yum install -y docker',
            'sudo systemctl enable docker',
            'sudo systemctl start docker',
            'sudo usermod -aG docker ec2-user',
            'sudo systemctl enable amazon-ssm-agent',
            'sudo systemctl start amazon-ssm-agent'
          ].join(' && ')
        ]
      },
      targets: [
        {
          key: 'InstanceIds',
          values: [influxDbInstance.instanceId]
        }
      ]
    });

    influxDbInstance.addUserData(
      '#!/bin/bash',
      'set -e',

      'DEVICE=/dev/xvdf',
      'MOUNT_POINT=/mnt/influxdb-data',

      // wait for /dev/xvdf
      'echo "Waiting for ${DEVICE} to appear..."',
      'for i in {1..30}; do',
      '  if [ -b "$DEVICE" ]; then',
      '    echo "Found ${DEVICE}"',
      '    break',
      '  fi',
      '  sleep 5',
      'done',

      'if [ ! -b "$DEVICE" ]; then',
      '  echo "ERROR: ${DEVICE} not found after waiting" >&2',
      '  exit 1',
      'fi',

      // format disc if fresh
      'if ! blkid "$DEVICE" > /dev/null 2>&1; then',
      '  mkfs -t xfs "$DEVICE"',
      'fi',

      // mount EBS volume
      'mkdir -p "$MOUNT_POINT"',
      'mount "$DEVICE" "$MOUNT_POINT"',
      'UUID=$(blkid -s UUID -o value "$DEVICE")',
      'grep -q "$MOUNT_POINT" /etc/fstab || echo "UUID=${UUID} ${MOUNT_POINT} xfs defaults,nofail 0 2" >> /etc/fstab'
    );

    influxDbInstance.addUserData(
      'set -xe',

      // wait for docker daemon
      'echo "Waiting for docker..."',
      'for i in {1..30}; do',
      '  if sudo docker info >/dev/null 2>&1; then',
      '    break',
      '  fi',
      '  sleep 5',
      'done',

      `aws ecr get-login-password --region ${cdk.Stack.of(this).region} \
        | docker login --username AWS --password-stdin ${process.env.AWS_ACCOUNT_ID}.dkr.ecr.${cdk.Stack.of(this).region}.amazonaws.com`,

      `docker pull ${process.env.AWS_ACCOUNT_ID}.dkr.ecr.${cdk.Stack.of(this).region}.amazonaws.com/stream-lines-influxdb:latest`,
      'set +xe',

      `docker run -d --restart=always --name influxdb \
        -p ${influxDbPort}:8086 \
        -v /mnt/influxdb-data:/var/lib/influxdb2 \
        -e DOCKER_INFLUXDB_INIT_MODE=${process.env.INFLUXDB_INIT_MODE} \
        -e DOCKER_INFLUXDB_INIT_USERNAME=${process.env.INFLUXDB_INIT_USERNAME} \
        -e DOCKER_INFLUXDB_INIT_PASSWORD=${process.env.INFLUXDB_INIT_PASSWORD} \
        -e DOCKER_INFLUXDB_INIT_ORG=${process.env.INFLUXDB_INIT_ORG} \
        -e DOCKER_INFLUXDB_INIT_BUCKET=${process.env.INFLUXDB_INIT_BUCKET} \
        -e DOCKER_INFLUXDB_INIT_RETENTION=${process.env.INFLUXDB_INIT_RETENTION} \
        -e DOCKER_INFLUXDB_INIT_ADMIN_TOKEN=${process.env.INFLUXDB_INIT_ADMIN_TOKEN} \
        ${process.env.AWS_ACCOUNT_ID}.dkr.ecr.${cdk.Stack.of(this).region}.amazonaws.com/stream-lines-influxdb:latest`
    );

    // local watchdog
    influxDbInstance.addUserData(
      'URL = "http://localhost:8086/health"',
      'NAME = "influxdb"',
      'while true; do',
      '  if !curl - sf "$URL" > /dev/null; then',
      '    docker restart "$NAME" > /dev/null 2 >& 1',
      '  fi',
      '  sleep 60',
      'done'
    );
  }
}
