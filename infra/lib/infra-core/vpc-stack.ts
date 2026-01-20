import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';

export class VpcStack extends cdk.NestedStack {
  readonly vpc: ec2.Vpc;

  constructor(scope: Construct, id: string, props?: cdk.NestedStackProps) {
    super(scope, id, props);

    this.vpc = new ec2.Vpc(this, 'StreamLinesVpc', {
      availabilityZones: ['eu-north-1a'],
      natGateways: 0, // only provision upon need in services stack
      subnetConfiguration: [
        {
          name: 'public',
          subnetType: ec2.SubnetType.PUBLIC
        },
        {
          name: 'private',
          subnetType: ec2.SubnetType.PRIVATE_WITH_EGRESS
        },
        {
          name: 'isolated',
          subnetType: ec2.SubnetType.PRIVATE_ISOLATED
        }
      ]
    });

    [
      { id: 'S3Endpoint', service: ec2.GatewayVpcEndpointAwsService.S3 },
      { id: 'DynamoDbGatewayEndpoint', service: ec2.GatewayVpcEndpointAwsService.DYNAMODB }
    ].forEach(({ id, service }) => {
      this.vpc.addGatewayEndpoint(id, {
        service,
        subnets: [{ subnetType: ec2.SubnetType.PRIVATE_ISOLATED }]
      });
    });
  }
}
