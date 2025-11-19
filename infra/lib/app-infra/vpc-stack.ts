import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';

export class VpcStack extends cdk.NestedStack {
  readonly vpc: ec2.Vpc;

  constructor(scope: Construct, id: string, props?: cdk.NestedStackProps) {
    super(scope, id, props);

    this.vpc = new ec2.Vpc(this, 'StreamLinesVpc', {
      maxAzs: 2,
      subnetConfiguration: [
        {
          name: 'public',
          subnetType: ec2.SubnetType.PUBLIC,
        },
        {
          name: 'isolated',
          subnetType: ec2.SubnetType.PRIVATE_ISOLATED,
        }
      ],
      natGateways: 0
    });

    new ec2.GatewayVpcEndpoint(this, 'S3Endpoint', {
      vpc: this.vpc,
      service: ec2.GatewayVpcEndpointAwsService.S3,
    });

    const ecrEndpointSg = new ec2.SecurityGroup(this, 'EcrEndpointSg', {
      vpc: this.vpc,
      allowAllOutbound: true,
    });

    ecrEndpointSg.addIngressRule(
      ec2.Peer.ipv4(this.vpc.vpcCidrBlock),
      ec2.Port.tcp(443),
      'Allow VPC to reach AWS Services' // without a NAT Gateway
    );

    [
      { id: 'CloudWatchLogsEndpoint', service: ec2.InterfaceVpcEndpointAwsService.CLOUDWATCH_LOGS },
      { id: 'EcrApiEndpoint', service: ec2.InterfaceVpcEndpointAwsService.ECR },
      { id: 'EcrDockerEndpoint', service: ec2.InterfaceVpcEndpointAwsService.ECR_DOCKER },
      { id: 'ElasticFilesystemEndpoint', service: ec2.InterfaceVpcEndpointAwsService.ELASTIC_FILESYSTEM },
      { id: 'KinesisStreamsEndpoint', service: ec2.InterfaceVpcEndpointAwsService.KINESIS_STREAMS },
    ].forEach(({ id, service }) => {
      new ec2.InterfaceVpcEndpoint(this, id, {
        vpc: this.vpc,
        service,
        privateDnsEnabled: true,
        securityGroups: [ecrEndpointSg],
        subnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      });
    });
  }
}
