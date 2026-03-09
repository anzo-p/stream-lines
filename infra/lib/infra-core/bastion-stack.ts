import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as iam from 'aws-cdk-lib/aws-iam';
import { Construct } from 'constructs';

export type BastionStackProps = cdk.NestedStackProps & {
  keyPairName: string;
  securityGroup: ec2.ISecurityGroup;
  ssmRole: iam.Role;
  vpc: ec2.IVpc;
};

export class BastionStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: BastionStackProps) {
    super(scope, id, props);

    const { keyPairName, securityGroup, ssmRole, vpc } = props;

    securityGroup.connections.allowFromAnyIpv4(ec2.Port.tcp(22), 'SSH access into Bastion');

    const keyPair = ec2.KeyPair.fromKeyPairName(this, 'BastionKeyPair', keyPairName);

    const bastionInstance = new ec2.Instance(this, 'BastionHost', {
      instanceType: new ec2.InstanceType('t4g.nano'),
      keyPair,
      machineImage: ec2.MachineImage.latestAmazonLinux2023({
        cpuType: ec2.AmazonLinuxCpuType.ARM_64
      }),
      role: ssmRole,
      securityGroup,
      vpc,
      vpcSubnets: { subnetType: ec2.SubnetType.PUBLIC }
    });

    cdk.Tags.of(bastionInstance).add('Name', 'influx-ssh-bastion');
  }
}
