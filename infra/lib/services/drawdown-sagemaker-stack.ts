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
  id: string;
  bucket: IBucket;
  outputPathKey: string;
  sagemakerRole: iam.IRole;
  trainingFileKey: string;
};

function defineTrainingPipeline(scope: Construct, args: TrainPipelineArgs, eventRole: iam.Role) {
  const trainJob = new tasks.SageMakerCreateTrainingJob(scope, `TrainXGB-${args.id}`, {
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
            s3Location: tasks.S3Location.fromBucket(args.bucket, args.trainingFileKey)
          }
        }
      }
    ],
    outputDataConfig: {
      s3OutputLocation: tasks.S3Location.fromBucket(args.bucket, args.outputPathKey)
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

  const stateMachine = new sfn.StateMachine(scope, `SageMakerStateMachine-${args.id}`, {
    definitionBody: sfn.DefinitionBody.fromChainable(
      trainJob.next(new sfn.Succeed(scope, `TrainingComplete-${args.id}`))
    )
  });

  eventRole.addToPolicy(
    new iam.PolicyStatement({
      actions: ['states:StartExecution'],
      resources: [stateMachine.stateMachineArn]
    })
  );

  new events.Rule(scope, `EventBridgeRule-${args.id}`, {
    eventPattern: {
      detail: {
        bucket: { name: [process.env.S3_APP_BUCKET!] },
        object: { key: [args.trainingFileKey] }
      },
      detailType: ['Object Created'],
      source: ['aws.s3']
    },
    targets: [
      new targets.SfnStateMachine(stateMachine, {
        input: events.RuleTargetInput.fromObject({
          s3Bucket: process.env.S3_APP_BUCKET!,
          s3Key: args.trainingFileKey
        }),
        role: eventRole
      })
    ]
  });

  // copy training job output to "latest" for easy discovery by predictor service
  const mlModelCopyFn = new NodejsFunction(scope, `MlModelCopyFn-${args.id}`, {
    entry: path.join(__dirname, '../../lambda/ml-model-copy.ts'),
    environment: {
      FILENAME: 'model.tar.gz',
      LATEST_KEY: `${args.outputPathKey}latest/output/${'model.tar.gz'}`
    },
    handler: 'handler',
    logRetention: logs.RetentionDays.ONE_WEEK,
    runtime: lambda.Runtime.NODEJS_20_X,
    timeout: cdk.Duration.seconds(30)
  });

  args.bucket.grantRead(mlModelCopyFn, `${args.outputPathKey}*`);
  args.bucket.grantPut(mlModelCopyFn, `${args.outputPathKey}*`);

  new events.Rule(scope, `OnModelArtifactCreated-${args.id}`, {
    eventPattern: {
      detail: {
        bucket: { name: [process.env.S3_APP_BUCKET!] },
        object: { key: [{ prefix: args.outputPathKey }] }
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

    const trainPipelineArgs: TrainPipelineArgs[] = [
      {
        id: 'drawdown-two-weeks',
        bucket: dataBucket,
        outputPathKey: `${process.env.NARWHAL_MODELS_PREFIX!}/drawdown-two-weeks/`,
        sagemakerRole,
        trainingFileKey: `${process.env.NARWHAL_TRAINING_DATA_PREFIX!}/drawdown-training-data-two-weeks/latest.csv.gz`
      },
      {
        id: 'drawdown-two-months',
        bucket: dataBucket,
        outputPathKey: `${process.env.NARWHAL_MODELS_PREFIX!}/drawdown-two-months/`,
        sagemakerRole,
        trainingFileKey: `${process.env.NARWHAL_TRAINING_DATA_PREFIX!}/drawdown-training-data-two-months/latest.csv.gz`
      }
    ];

    trainPipelineArgs.forEach((args) => defineTrainingPipeline(this, args, eventRole));
  }
}
