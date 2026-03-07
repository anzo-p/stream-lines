import * as cdk from 'aws-cdk-lib';
import * as events from 'aws-cdk-lib/aws-events';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as sfn from 'aws-cdk-lib/aws-stepfunctions';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as tasks from 'aws-cdk-lib/aws-stepfunctions-tasks';
import * as path from 'path';
import { Construct } from 'constructs';
import { IBucket } from 'aws-cdk-lib/aws-s3';
import { NodejsFunction } from 'aws-cdk-lib/aws-lambda-nodejs';

type TrainPipelineArgs = {
  name: string;
  bucket: IBucket;
  copyTargetDirname: string;
  modelOutputDirname: string;
  sagemakerRole: iam.IRole;
  trainingFileFullPath: string;
};

function defineTrainingPipeline(scope: Construct, args: TrainPipelineArgs, eventRole: iam.Role) {
  const trainJob = new tasks.SageMakerCreateTrainingJob(scope, `TrainXGB-${args.name}`, {
    algorithmSpecification: {
      trainingImage: tasks.DockerImage.fromRegistry(process.env.SAGEMAKER_XGBOOST_IMAGE!),
      trainingInputMode: tasks.InputMode.PIPE
    },
    hyperparameters: {
      colsample_bytree: '0.8',
      eta: '0.1',
      max_depth: '5',
      num_round: '200',
      objective: 'reg:squarederror',
      subsample: '0.8'
    },
    inputDataConfig: [
      {
        channelName: 'train',
        contentType: 'text/csv',
        compressionType: tasks.CompressionType.GZIP,
        dataSource: {
          s3DataSource: {
            s3DataType: tasks.S3DataType.S3_PREFIX,
            s3Location: tasks.S3Location.fromBucket(args.bucket, args.trainingFileFullPath)
          }
        }
      }
    ],
    outputDataConfig: {
      s3OutputLocation: tasks.S3Location.fromBucket(args.bucket, args.modelOutputDirname)
    },
    resourceConfig: {
      instanceCount: 1,
      instanceType: new cdk.aws_ec2.InstanceType('m5.large'),
      volumeSize: cdk.Size.gibibytes(30)
    },
    role: args.sagemakerRole,
    stoppingCondition: {
      maxRuntime: cdk.Duration.minutes(15)
    },
    trainingJobName: sfn.JsonPath.format('xgb-{}', sfn.JsonPath.uuid())
  });

  // start sagemaker job upon training data upload

  const stateMachine = new sfn.StateMachine(scope, `SageMakerStateMachine-${args.name}`, {
    definitionBody: sfn.DefinitionBody.fromChainable(
      trainJob.next(new sfn.Succeed(scope, `TrainingComplete-${args.name}`))
    )
  });

  eventRole.addToPolicy(
    new iam.PolicyStatement({
      actions: ['states:StartExecution'],
      resources: [stateMachine.stateMachineArn]
    })
  );

  new events.Rule(scope, `EventBridgeRule-${args.name}`, {
    eventPattern: {
      detail: {
        bucket: { name: [process.env.S3_APP_BUCKET!] },
        object: { key: [args.trainingFileFullPath] }
      },
      detailType: ['Object Created'],
      source: ['aws.s3']
    },
    targets: [
      new targets.SfnStateMachine(stateMachine, {
        input: events.RuleTargetInput.fromObject({
          s3Bucket: process.env.S3_APP_BUCKET!,
          s3Key: args.trainingFileFullPath
        }),
        role: eventRole
      })
    ]
  });

  // copy training job output to "latest" for easy discovery by predictor service

  errorOnPathOverlap(args.modelOutputDirname, args.copyTargetDirname);

  const mlModelCopyFn = new NodejsFunction(scope, `MlModelCopyFn-${args.name}`, {
    entry: path.join(__dirname, '../../lambda/ml-model-copy.ts'),
    environment: {
      FILENAME: 'model.tar.gz',
      SOURCE_PATH: args.modelOutputDirname,
      TARGET_PATH: args.copyTargetDirname
    },
    handler: 'handler',
    logRetention: logs.RetentionDays.ONE_WEEK,
    runtime: lambda.Runtime.NODEJS_20_X,
    timeout: cdk.Duration.seconds(30)
  });

  args.bucket.grantRead(mlModelCopyFn, `${args.modelOutputDirname}*`);
  args.bucket.grantPut(mlModelCopyFn, `${args.copyTargetDirname}*`);

  new events.Rule(scope, `OnModelArtifactCreated-${args.name}`, {
    eventPattern: {
      detail: {
        bucket: { name: [process.env.S3_APP_BUCKET!] },
        object: { key: [{ prefix: args.modelOutputDirname }] }
      },
      detailType: ['Object Created'],
      source: ['aws.s3']
    },
    targets: [new targets.LambdaFunction(mlModelCopyFn)]
  });
}

export class DrawdownSagemakerStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props?: cdk.NestedStackProps) {
    super(scope, id, props);

    // EventBridge requires: <bucket> -> props -> EventBridge -> Send notifs EventBridge -> On
    const bucketName = process.env.S3_APP_BUCKET!;
    const dataBucket = s3.Bucket.fromBucketName(this, 'AppBucket', bucketName);

    const sagemakerRole = new iam.Role(this, 'SageMakerTrainingRole', {
      assumedBy: new iam.ServicePrincipal('sagemaker.amazonaws.com')
    });
    sagemakerRole.addToPolicy(
      new iam.PolicyStatement({
        actions: ['s3:GetObject', 's3:ListBucket', 's3:PutObject'],
        resources: [`arn:aws:s3:::${bucketName}`, `arn:aws:s3:::${bucketName}/*`]
      })
    );

    const eventRole = new iam.Role(this, 'EventBridgeStartSfnRole', {
      assumedBy: new iam.ServicePrincipal('events.amazonaws.com')
    });

    const trainPipelineArgs: TrainPipelineArgs[] = ['drawdown-two-weeks', 'drawdown-five-weeks'].map((name) => ({
      bucket: dataBucket,
      copyTargetDirname: `${process.env.NARWHAL_MODELS_LATEST_PATH!}/${name}/`,
      modelOutputDirname: `${process.env.NARWHAL_MODELS_RUNS_PATH!}/${name}/`,
      name,
      sagemakerRole,
      trainingFileFullPath: `${process.env.NARWHAL_TRAINING_DATA_PATH!}/${name}/latest.csv.gz`
    }));

    trainPipelineArgs.forEach((args) => defineTrainingPipeline(this, args, eventRole));
  }
}

function errorOnPathOverlap(sourcePath: string, targetPath: string) {
  if (normalizePrefix(targetPath).startsWith(normalizePrefix(sourcePath))) {
    throw new Error(`sourcePath "${sourcePath}" and targetPath "${targetPath}" must not overlap.`);
  }
}

function normalizePrefix(prefix: string): string {
  const trimmed = prefix.trim().replace(/^\/+|\/+$/g, '');
  return trimmed === '' ? '' : `${trimmed}/`;
}
