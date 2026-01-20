import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as cloudfront from 'aws-cdk-lib/aws-cloudfront';
import * as origins from 'aws-cdk-lib/aws-cloudfront-origins';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as targets from 'aws-cdk-lib/aws-route53-targets';
import * as acm from 'aws-cdk-lib/aws-certificatemanager';

export class FrontendStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    const domainName = `${process.env.WEBAPP_ADDRESS}`;
    const zone = route53.HostedZone.fromLookup(this, 'HostedZone', {
      domainName: `${process.env.WEBAPP_DOMAIN}`
    });

    const certificate = acm.Certificate.fromCertificateArn(
      this,
      'FrontendCert',
      `arn:aws:acm:us-east-1:${process.env.AWS_ACCOUNT_ID}:certificate/${process.env.ACM_CLOUDFRONT_CERT}`
    );

    const bucket = s3.Bucket.fromBucketName(this, 'FrontendBucket', `${process.env.S3_WEBAPP_BUCKET}`);

    const distribution = new cloudfront.Distribution(this, 'FrontendDist', {
      certificate,
      defaultBehavior: {
        origin: new origins.S3Origin(bucket),
        viewerProtocolPolicy: cloudfront.ViewerProtocolPolicy.REDIRECT_TO_HTTPS
      },
      defaultRootObject: 'index.html',
      domainNames: [domainName]
    });

    new route53.ARecord(this, 'FrontendAlias', {
      recordName: domainName,
      target: route53.RecordTarget.fromAlias(new targets.CloudFrontTarget(distribution)),
      zone
    });

    new cdk.CfnOutput(this, 'BucketName', { value: bucket.bucketName });
    new cdk.CfnOutput(this, 'CloudFrontUrl', { value: distribution.domainName });
  }
}
