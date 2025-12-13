import * as cdk from 'aws-cdk-lib';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import { Construct } from 'constructs';

export class NatGatewayStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, vpc: ec2.Vpc, props?: cdk.NestedStackProps) {
    super(scope, id, props);

    const natEip = new ec2.CfnEIP(this, 'NatEip', {
      domain: 'vpc'
    });

    const natGateway = new ec2.CfnNatGateway(this, 'NatGateway', {
      subnetId: vpc.publicSubnets[0].subnetId,
      allocationId: natEip.attrAllocationId
    });

    for (const subnet of vpc.privateSubnets) {
      new ec2.CfnRoute(this, `NatRoute${subnet.node.id}`, {
        routeTableId: subnet.routeTable.routeTableId,
        destinationCidrBlock: '0.0.0.0/0',
        natGatewayId: natGateway.ref
      });
    }
  }
}
