import * as cdk from 'aws-cdk-lib';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as servicediscovery from 'aws-cdk-lib/aws-servicediscovery';
import { Construct } from 'constructs';

export class EcsClusterStack extends cdk.NestedStack {
  readonly ecsCluster: ecs.Cluster;
  readonly influxDbRepositoryName: string = 'stream-lines-influxdb';
  readonly ingestRepositoryName: string = 'stream-lines-ingest';

  constructor(scope: Construct, id: string, vpc: ec2.Vpc, props?: cdk.NestedStackProps) {
    super(scope, id, props);

    this.ecsCluster = new ecs.Cluster(this, 'StreamLinesEcsCluster', {
      defaultCloudMapNamespace: {
        name: `${process.env.PRIVATE_NAMESPACE}`,
        type: servicediscovery.NamespaceType.DNS_PRIVATE
      },
      vpc
    });

    this.ecsCluster.enableFargateCapacityProviders();
  }
}
