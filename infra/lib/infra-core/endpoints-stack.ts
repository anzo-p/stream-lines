import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';

/*
  # 1. initial deploy
  cdk deploy StreamLines-Infra -c enableIEndpoints=true

  # 2. deploy to remove endpoints upon successful provisioning
  cdk deploy StreamLines-Infra

  This stack is an optional way to connect to services within AWS without using a NAT Gateway.
  This is iportant as to keep costs down the infra-core stack does not deploy a NAT.
  Yet the EC2 to host InfluxDB needs Internet to update Linux, install Docker, pull image from ECR, etc.
*/

export class InterfaceEndpointsStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, vpc: ec2.Vpc, props?: cdk.NestedStackProps) {
    super(scope, id, props);

    const vpnEndpointSg = new ec2.SecurityGroup(this, 'EcrEndpointSg', {
      vpc,
      allowAllOutbound: true
    });

    vpnEndpointSg.addIngressRule(ec2.Peer.ipv4(vpc.vpcCidrBlock), ec2.Port.tcp(443), 'Allow VPC to reach AWS Services');

    // Whenever you need to pull new images from AWS ECR
    const ec2EcrEndpints = [
      { id: 'Ec2Endpoint', service: ec2.InterfaceVpcEndpointAwsService.EC2 },
      { id: 'EcrApiEndpoint', service: ec2.InterfaceVpcEndpointAwsService.ECR },
      { id: 'EcrDockerEndpoint', service: ec2.InterfaceVpcEndpointAwsService.ECR_DOCKER }
    ];

    // Whenever you need to connect into the instance via AWS Session Manager
    const ssmEnpoints = [
      { id: 'SsmEndpoint', service: ec2.InterfaceVpcEndpointAwsService.SSM },
      { id: 'SsmMessagesEndpoint', service: ec2.InterfaceVpcEndpointAwsService.SSM_MESSAGES },
      { id: 'Ec2MessagesEndpoint', service: ec2.InterfaceVpcEndpointAwsService.EC2_MESSAGES }
    ];

    [
      ...ec2EcrEndpints,
      ...ssmEnpoints,
      { id: 'CloudWatchLogsEndpoint', service: ec2.InterfaceVpcEndpointAwsService.CLOUDWATCH_LOGS },
      { id: 'KinesisStreamsEndpoint', service: ec2.InterfaceVpcEndpointAwsService.KINESIS_STREAMS }
    ].forEach(({ id, service }) => {
      const endpoint = new ec2.InterfaceVpcEndpoint(this, id, {
        vpc,
        service,
        privateDnsEnabled: true,
        securityGroups: [vpnEndpointSg],
        subnets: { subnetType: ec2.SubnetType.PRIVATE_ISOLATED }
      });
      cdk.Tags.of(endpoint).add('Name', id);
    });
  }
}
