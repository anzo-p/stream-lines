import * as cdk from 'aws-cdk-lib';
import * as acm from 'aws-cdk-lib/aws-certificatemanager';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as targets from 'aws-cdk-lib/aws-route53-targets';
import { Construct } from 'constructs';

export class AlbStack extends cdk.NestedStack {
  readonly backendAlbListener: elbv2.ApplicationListener;
  readonly influxDBAlbListener: elbv2.ApplicationListener;

  constructor(
    scope: Construct,
    id: string,
    vpc: ec2.Vpc,
    props?: cdk.NestedStackProps
  ) {
    super(scope, id, props);

    const zone = route53.HostedZone.fromLookup(this, 'HostedZone', {
      domainName: 'anzop.net'
    });

    const alb_certificate = acm.Certificate.fromCertificateArn(
      this,
      'Certificate',
      `arn:aws:acm:${process.env.AWS_REGION}:${process.env.AWS_ACCOUNT}:certificate/${process.env.ACM_CERT}`
    );

    const influxDBAlb = new elbv2.ApplicationLoadBalancer(this, 'InfluxDBAlb', {
      vpc,
      internetFacing: true
    });

    new route53.ARecord(this, 'InfluxAlbAliasRecord', {
      zone,
      recordName: `${process.env.INFLUXDB_SUBDOMAIN}`,
      target: route53.RecordTarget.fromAlias(
        new targets.LoadBalancerTarget(influxDBAlb)
      )
    });

    this.influxDBAlbListener = influxDBAlb.addListener('InfluxDBAlbListener', {
      port: 443,
      protocol: elbv2.ApplicationProtocol.HTTPS,
      certificates: [alb_certificate]
    });

    const backendAlb = new elbv2.ApplicationLoadBalancer(this, 'BackendAlb', {
      vpc,
      internetFacing: true
    });

    new route53.ARecord(this, 'BackendAlbAliasRecord', {
      zone,
      recordName: `${process.env.BACKEND_SUBDOMAIN}`,
      target: route53.RecordTarget.fromAlias(
        new targets.LoadBalancerTarget(backendAlb)
      )
    });

    this.backendAlbListener = backendAlb.addListener('BackendAlbListener', {
      port: 443,
      protocol: elbv2.ApplicationProtocol.HTTPS,
      certificates: [alb_certificate]
    });
  }
}
