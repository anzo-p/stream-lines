import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';
import { EcsClusterStack } from './ecs-cluster-stack';
import { InfluxDbStack } from './influxdb-ec2-stack';
import { BastionStack } from './bastion-stack';
import { InterfaceEndpointsStack } from './endpoints-stack';
import { VpcStack } from './vpc-stack';

export class InfraCoreStack extends cdk.Stack {
  readonly vpc: ec2.Vpc;
  readonly serviceSecurityGroups: Record<string, ec2.SecurityGroup>;
  readonly ecsCluster: ecs.Cluster;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    this.vpc = new VpcStack(this, 'VpcStack').vpc;

    if (this.node.tryGetContext('enableIEndpoints') === 'true') {
      new InterfaceEndpointsStack(this, 'InterfaceEndpointsStack', this.vpc);
    }

    this.ecsCluster = new EcsClusterStack(this, 'EcsClusterStack', this.vpc).ecsCluster;

    const sgSpecsBastion = [{ id: 'bastion', name: 'BastionSecurityGroup' }];

    const sgSpecsDbConnServices = [
      { id: 'currents', name: 'CurrentsSecurityGroup' },
      { id: 'gather', name: 'GatherSecurityGroup' },
      { id: 'ripples', name: 'RipplesSecurityGroup' }
    ];

    const sgSpecsOtherServices = [
      //{ id: 'backend', name: 'BackendSecurityGroup' },
      { id: 'ingest', name: 'IngestSecurityGroup' }
    ];

    this.serviceSecurityGroups = Object.fromEntries(
      [...sgSpecsBastion, ...sgSpecsDbConnServices, ...sgSpecsOtherServices].map(({ id, name }) => {
        const sg = new ec2.SecurityGroup(this, name, {
          vpc: this.vpc,
          allowAllOutbound: true
        });
        return [id, sg];
      })
    );

    const ssmRole = new iam.Role(this, 'Ec2SsmRole', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com')
    });

    ['AmazonEC2ContainerRegistryReadOnly', 'AmazonSSMManagedInstanceCore', 'CloudWatchAgentServerPolicy'].forEach(
      (policyName) => {
        ssmRole.addManagedPolicy(iam.ManagedPolicy.fromAwsManagedPolicyName(policyName));
      }
    );

    new BastionStack(this, 'BastionStack', {
      bastionSecurityGroup: this.serviceSecurityGroups['bastion'],
      ssmRole,
      vpc: this.vpc
    });

    new InfluxDbStack(this, 'InfluxDbStack', {
      bastionSecurityGroup: this.serviceSecurityGroups['bastion'],
      connectingServiceSGs: sgSpecsDbConnServices.map(({ id }) => ({ id, sg: this.serviceSecurityGroups[id] })),
      ecsCluster: this.ecsCluster,
      ssmRole,
      vpc: this.vpc
    });
  }
}
