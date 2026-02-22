import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';
import { EcsClusterStack } from './ecs-cluster-stack';
import { InfluxDbStack } from './influxdb-stack';
import { BastionStack } from './bastion-stack';
import { InterfaceEndpointsStack } from './endpoints-stack';
import { VpcStack } from './vpc-stack';

export class InfraCoreStack extends cdk.Stack {
  readonly vpc: ec2.Vpc;
  readonly ecsCluster: ecs.Cluster;

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
      securityGroup: bastionSg,
      ssmRole,
      vpc: this.vpc
    });

    new InfluxDbStack(this, 'InfluxDbStack', {
      ecsCluster: this.ecsCluster,
      securityGroup: influxSg,
      ssmRole,
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
