import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import { DynamodbStack } from './dynamodb-stack';
import { EcrStack } from './ecr-stack';
import { EfsStack } from './efs-stack';
import { S3Stack } from './s3-stack';

export class FoundationsStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    new DynamodbStack(this, 'DynamodbStack');
    new EcrStack(this, 'EcrStack');
    new EfsStack(this, 'EfsStack');
    new S3Stack(this, 'S3Stack');
  }

  /*
    - obtain file system id into .env
    - detach networking to release the fs as mountable for influx stack later on
  */
}
