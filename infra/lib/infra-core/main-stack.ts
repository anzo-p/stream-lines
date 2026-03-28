import * as cdk from 'aws-cdk-lib';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';
import { BastionStack } from './bastion-stack';
import { EcsClusterStack } from './ecs-cluster-stack';
import { InfluxDbStack } from './influxdb-stack';
import { InterfaceEndpointsStack } from './endpoints-stack';
import { VpcStack } from './vpc-stack';

export class InfraCoreStack extends cdk.Stack {
  readonly ecsCluster: ecs.Cluster;
  readonly vpc: ec2.Vpc;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    this.vpc = new VpcStack(this, 'VpcStack').vpc;

    if (this.node.tryGetContext('enableIEndpoints') === 'true') {
      new InterfaceEndpointsStack(this, 'InterfaceEndpointsStack', this.vpc);
    }

    this.ecsCluster = new EcsClusterStack(this, 'EcsClusterStack', this.vpc).ecsCluster;

    const bastionSg = new ec2.SecurityGroup(this, 'BastionSecurityGroup', {
      vpc: this.vpc,
      allowAllOutbound: true
    });
    const influxSg = new ec2.SecurityGroup(this, 'InfluxDbSecurityGroup', {
      vpc: this.vpc,
      allowAllOutbound: true
    });
    [
      { port: ec2.Port.tcp(22), description: 'Allow Jump Bastion access to InfluxDB instance' },
      {
        port: ec2.Port.tcp(Number(process.env.INFLUXDB_SERVER_PORT!)),
        description: 'Allow Jump Bastion access to machine running influxDB'
      }
    ].forEach(({ port, description }) => {
      influxSg.addIngressRule(bastionSg, port, description);
    });

    const ssmRole = new iam.Role(this, 'Ec2SsmRole', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com')
    });

    ['AmazonEC2ContainerRegistryReadOnly', 'AmazonSSMManagedInstanceCore', 'CloudWatchAgentServerPolicy'].forEach(
      (policyName) => {
        ssmRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName(policyName));
      }
    );

    new BastionStack(this, 'BastionStack', {
      amiId: process.env.AMI_BASTION!,
      keyPairName: process.env.KEY_NAME_BASTION!,
      securityGroup: bastionSg,
      ssmRole,
      vpc: this.vpc
    });

    new InfluxDbStack(this, 'InfluxDbStack', {
      amiId: process.env.AMI_INFLUXDB!,
      ecsCluster: this.ecsCluster,
      initBucket: process.env.INFLUXDB_INIT_BUCKET!,
      initMode: process.env.INFLUXDB_INIT_MODE!,
      initOrg: process.env.INFLUXDB_INIT_ORG!,
      initPassword: process.env.INFLUXDB_INIT_PASSWORD!,
      initRetention: process.env.INFLUXDB_INIT_RETENTION!,
      initUsername: process.env.INFLUXDB_INIT_USERNAME!,
      keyPairName: process.env.KEY_NAME_INFLUXDB!,
      port: Number(process.env.INFLUXDB_SERVER_PORT!),
      securityGroup: influxSg,
      ssmRole,
      volumeId: process.env.INFLUXDB_FILE_SYSTEM_ID!,
      vpc: this.vpc
    });

    new cdk.CfnOutput(this, 'BastionSgIdOut', {
      value: bastionSg.securityGroupId,
      exportName: `${cdk.Stack.of(this).stackName}:BastionSgId`
    });

    new cdk.CfnOutput(this, 'InfluxSgIdOut', {
      value: influxSg.securityGroupId,
      exportName: `${cdk.Stack.of(this).stackName}:InfluxSgId`
    });
  }
}
