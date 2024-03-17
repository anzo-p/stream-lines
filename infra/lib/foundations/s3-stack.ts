import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';

export class S3Stack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const bucketName = process.env.S3_APP_BUCKET!;

    new cdk.aws_s3.Bucket(this, 'S3AppBucket', {
      bucketName,
      removalPolicy: cdk.RemovalPolicy.RETAIN,
      autoDeleteObjects: false
    });
  }
}
