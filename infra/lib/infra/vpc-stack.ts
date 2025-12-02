import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';

export class VpcStack extends cdk.NestedStack {
  readonly vpc: ec2.Vpc;

  constructor(scope: Construct, id: string, props?: cdk.NestedStackProps) {
    super(scope, id, props);

    this.vpc = new ec2.Vpc(this, 'StreamLinesVpc', {
      availabilityZones: ['eu-north-1a', 'eu-north-1b'],
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

    const vpnEndpointSg = new ec2.SecurityGroup(this, 'EcrEndpointSg', {
      vpc: this.vpc,
      allowAllOutbound: true,
    });

    vpnEndpointSg.addIngressRule(
      ec2.Peer.ipv4(this.vpc.vpcCidrBlock),
      ec2.Port.tcp(443),
      'Allow VPC to reach AWS Services' // without a NAT Gateway
    );

    // Whenever you need to pull new images from AWS ECR
    const ec2EcrEndpints = [
      { id: 'Ec2Endpoint', service: ec2.InterfaceVpcEndpointAwsService.EC2 },
      { id: 'EcrApiEndpoint', service: ec2.InterfaceVpcEndpointAwsService.ECR },
      { id: 'EcrDockerEndpoint', service: ec2.InterfaceVpcEndpointAwsService.ECR_DOCKER },
    ];

    // Whenever you need to connect into the instance via AWS Session Manager
    const ssmEnpoints = [
      { id: 'SsmEndpoint', service: ec2.InterfaceVpcEndpointAwsService.SSM },
      { id: 'SsmMessagesEndpoint', service: ec2.InterfaceVpcEndpointAwsService.SSM_MESSAGES },
      { id: 'Ec2MessagesEndpoint', service: ec2.InterfaceVpcEndpointAwsService.EC2_MESSAGES },
    ];

    [
      // Toggle on when deplopying and off after successful deployment
      //...ec2EcrEndpints,
      //...ssmEnpoints,
      { id: 'CloudWatchLogsEndpoint', service: ec2.InterfaceVpcEndpointAwsService.CLOUDWATCH_LOGS },
      { id: 'KinesisStreamsEndpoint', service: ec2.InterfaceVpcEndpointAwsService.KINESIS_STREAMS },
    ].forEach(({ id, service }) => {
      const endpoint = new ec2.InterfaceVpcEndpoint(this, id, {
        vpc: this.vpc,
        service,
        privateDnsEnabled: true,
        securityGroups: [vpnEndpointSg],
        subnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED },
      });
      cdk.Tags.of(endpoint).add("Name", id);
    });
  }
}
