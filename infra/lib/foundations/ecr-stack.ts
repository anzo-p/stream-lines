import * as cdk from 'aws-cdk-lib';
import * as ecr from 'aws-cdk-lib/aws-ecr';
import { Construct } from 'constructs';

export class EcrStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    ['backend', 'compute', 'dashboard', 'influxdb', 'ingest'].forEach(
      (repo) => {
        const repositoryName = `stream-lines-${repo}`;

        new ecr.Repository(this, `S3-${repo}`, {
          repositoryName,
          removalPolicy: cdk.RemovalPolicy.RETAIN
        });
      }
    );
  }
}
