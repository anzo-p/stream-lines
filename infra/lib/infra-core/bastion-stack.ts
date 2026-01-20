import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export type BastionStackProps = cdk.NestedStackProps & {
  bastionSecurityGroup: ec2.SecurityGroup;
  ssmRole: iam.Role;
  vpc: ec2.Vpc;
};

export class BastionStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: BastionStackProps) {
    super(scope, id, props);

    const { vpc, bastionSecurityGroup, ssmRole } = props;

    bastionSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(22), 'SSH access into Bastion');

    const bastionInstance = new ec2.Instance(this, 'BastionHost', {
      instanceType: new ec2.InstanceType('t4g.nano'),
      keyName: process.env.KEY_NAME_BASTION,
      machineImage: ec2.MachineImage.latestAmazonLinux2023({
        cpuType: ec2.AmazonLinuxCpuType.ARM_64
      }),
      role: ssmRole,
      securityGroup: bastionSecurityGroup,
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC }
    });

    cdk.Tags.of(bastionInstance).add('Name', 'influx-ssh-bastion');
  }
}
