import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import { Construct } from 'constructs';
import { EcsClusterStack } from './ecs-cluster-stack';
import { InfluxDbStack } from './influxdb-ec2-stack';
import { VpcStack } from './vpc-stack';
import { JumpBastionStack } from './jump-bastion-stack';

export class InfraCoreStack extends cdk.Stack {
  readonly vpc: ec2.Vpc;
  readonly serviceSecurityGroups: Record<string, ec2.SecurityGroup>;
  readonly ecsCluster: ecs.Cluster;

  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    this.vpc = new VpcStack(this, 'VpcStack').vpc;

    this.ecsCluster = new EcsClusterStack(
      this,
      'EcsClusterStack',
      this.vpc,
    ).ecsCluster;

    const sgSpecsDbConnServices = [
      { id: 'currents', name: 'CurrentsSecurityGroup' },
      { id: 'ripples', name: 'RipplesSecurityGroup' },
      { id: 'gather', name: 'GatherSecurityGroup' },
    ];

    const sgSpecsOtherServices = [
      //{ id: 'backend', name: 'BackendSecurityGroup' },
      { id: 'ingest', name: 'IngestSecurityGroup' },
    ];

    this.serviceSecurityGroups = Object.fromEntries(
      [...sgSpecsDbConnServices, ...sgSpecsOtherServices]
        .map(({ id, name }) => {
          const sg = new ec2.SecurityGroup(this, name, {
            vpc: this.vpc,
            allowAllOutbound: true,
          });
          return [id, sg];
        })
    );

    const bastionSecurityGroup = new ec2.SecurityGroup(this, 'BastionSecurityGroup', {
      vpc: this.vpc,
      allowAllOutbound: true,
    });

    new JumpBastionStack(
      this,
      'JumpBastionStack',
      this.vpc,
      bastionSecurityGroup,
    );

    new InfluxDbStack(
      this,
      'InfluxDbStack',
      this.vpc,
      this.ecsCluster,
      bastionSecurityGroup,
      sgSpecsDbConnServices.map(({ id }) => ({ id, sg: this.serviceSecurityGroups[id] }))
    );
  }
}
