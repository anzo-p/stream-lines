import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as efs from 'aws-cdk-lib/aws-efs';
import { Construct } from 'constructs';

export class EfsStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const vpc = ec2.Vpc.fromLookup(this, 'Vpc', {
      isDefault: true
    });

    const fileSystem = new efs.FileSystem(
      this,
      'StreamLinesInfluxDbFileSystem',
      {
        vpc,
        removalPolicy: cdk.RemovalPolicy.RETAIN
      }
    );

    fileSystem.addAccessPoint('MyAccessPoint', {
      posixUser: {
        uid: '1000',
        gid: '1000'
      },
      createAcl: {
        ownerUid: '1000',
        ownerGid: '1000',
        permissions: '0755'
      },
      path: '/var/lib/influxdb2'
    });
  }
}
