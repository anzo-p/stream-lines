import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as elbv2 from 'aws-cdk-lib/aws-elasticloadbalancingv2';
import { Construct } from 'constructs';

export class AlbStack extends cdk.NestedStack {
  readonly backendAlbListener: elbv2.ApplicationListener;
  readonly influxDBAlbDns: string;
  readonly influxDBAlbListener: elbv2.ApplicationListener;

  constructor(
    scope: Construct,
    id: string,
    vpc: ec2.Vpc,
    props?: cdk.NestedStackProps
  ) {
    super(scope, id, props);

    const backendAlb = new elbv2.ApplicationLoadBalancer(this, 'BackendAlb', {
      vpc,
      internetFacing: true
    });

    this.backendAlbListener = backendAlb.addListener('BackendAlbListener', {
      port: 80,
      protocol: elbv2.ApplicationProtocol.HTTP
    });

    const influxDBAdminAlb = new elbv2.ApplicationLoadBalancer(
      this,
      'InfluxDBAlb',
      {
        vpc,
        internetFacing: true
      }
    );
    this.influxDBAlbDns = influxDBAdminAlb.loadBalancerDnsName;

    this.influxDBAlbListener = influxDBAdminAlb.addListener(
      'InfluxDBAlbListener',
      {
        port: 80,
        protocol: elbv2.ApplicationProtocol.HTTP
      }
    );
  }
}
