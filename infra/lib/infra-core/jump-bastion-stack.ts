import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';

export class JumpBastionStack extends cdk.NestedStack {
  constructor(
    scope: Construct,
    id: string,
    vpc: ec2.Vpc,
    bastionSecurityGroup: ec2.SecurityGroup,
    props?: cdk.NestedStackProps
  ) {
    super(scope, id, props);

    bastionSecurityGroup.addIngressRule(ec2.Peer.anyIpv4(), ec2.Port.tcp(22), 'SSH access into Bastion');

    const bastionInstance = new ec2.Instance(this, 'BastionHost', {
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC },
      instanceType: new ec2.InstanceType('t4g.nano'),
      machineImage: ec2.MachineImage.latestAmazonLinux2023({
        cpuType: ec2.AmazonLinuxCpuType.ARM_64
      }),
      securityGroup: bastionSecurityGroup,
      keyName: `${process.env.BASTION_KEY_NAME}`
    });

    cdk.Tags.of(bastionInstance).add('Name', 'influx-ssh-bastion');
  }
}
