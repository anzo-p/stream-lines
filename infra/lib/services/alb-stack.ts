import * as cdk from 'aws-cdk-lib';
import * as acm from 'aws-cdk-lib/aws-certificatemanager';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import * as route53 from 'aws-cdk-lib/aws-route53';
import * as targets from 'aws-cdk-lib/aws-route53-targets';
import { Construct } from 'constructs';

export class AlbStack extends cdk.NestedStack {
  readonly backendAlbListener: elbv2.ApplicationListener;
  readonly dashboardAlbListener: elbv2.ApplicationListener;
  readonly influxDbAlbListener: elbv2.ApplicationListener;

  constructor(scope: Construct, id: string, vpc: ec2.Vpc, props?: cdk.NestedStackProps) {
    super(scope, id, props);

    // backend
    const zone = route53.HostedZone.fromLookup(this, 'HostedZone', {
      domainName: 'anzop.net'
    });

    const backendAlb = new elbv2.ApplicationLoadBalancer(this, 'BackendAlb', {
      internetFacing: true,
      vpc
    });

    new route53.ARecord(this, 'BackendAlbAliasRecord', {
      recordName: `${process.env.BACKEND_SUBDOMAIN}`,
      target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(backendAlb)),
      zone
    });

    const backendAlbCertificate = acm.Certificate.fromCertificateArn(
      this,
      'BackendCertificate',
      `arn:aws:acm:${process.env.AWS_REGION}:${process.env.AWS_ACCOUNT_ID}:certificate/${process.env.ACM_BACKEND_CERT}`
    );

    this.backendAlbListener = backendAlb.addListener('BackendAlbListener', {
      certificates: [backendAlbCertificate],
      port: 443,
      protocol: elbv2.ApplicationProtocol.HTTPS
    });

    /*
    // dashboard
    const dashboardAlb = new elbv2.ApplicationLoadBalancer(this, 'DashboardAlb', {
      internetFacing: true,
      vpc
    });

    new route53.ARecord(this, 'DashboardAlbAliasRecord', {
      recordName: `${process.env.DASHBOARD_SUBDOMAIN}`,
      target: route53.RecordTarget.fromAlias(new targets.LoadBalancerTarget(dashboardAlb)),
      zone
    });

    const webappAlbCertificate = acm.Certificate.fromCertificateArn(
      this,
      'WebAppCertificate',
      `arn:aws:acm:${process.env.AWS_REGION}:${process.env.AWS_ACCOUNT_ID}:certificate/${process.env.ACM_WEBAPP_CERT}`
    );

    this.dashboardAlbListener = dashboardAlb.addListener('DashboardAlbListenerHttps', {
      certificates: [webappAlbCertificate],
      port: 443,
      protocol: elbv2.ApplicationProtocol.HTTPS
    });

    dashboardAlb.addListener('DashboardAlbListenerRedirectToHttps', {
      defaultAction: elbv2.ListenerAction.redirect({
        host: '#{host}',
        path: '/#{path}',
        permanent: true,
        port: '443',
        protocol: 'HTTPS'
      }),
      port: 80,
      protocol: elbv2.ApplicationProtocol.HTTP
    });
    */
  }
}
