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
import { NodejsFunction } from 'aws-cdk-lib/aws-lambda-nodejs';

export type DrawdownSagemakerStackProps = cdk.StackProps & {
  appBucket: s3.IBucket;
  modelsLatestDirname: string;
  modelsRunsDirname: string;
  trainingDirname: string;
  xgboostImage: string;
};

type TrainPipelineArgs = {
  bucket: s3.IBucket;
  copyTargetDirname: string;
  modelOutputDirname: string;
  name: string;
  trainingFileFullPath: string;
};

function provisionTrainingPipeline(
  scope: Construct,
  args: TrainPipelineArgs,
  eventRole: iam.IRole,
  sagemakerRole: iam.IRole,
  xgboostImage: string
) {
  const trainJob = new tasks.SageMakerCreateTrainingJob(scope, `TrainXGB-${args.name}`, {
    algorithmSpecification: {
      trainingImage: tasks.DockerImage.fromRegistry(xgboostImage),
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
    role: sagemakerRole,
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
  stateMachine.grantStartExecution(eventRole);

  new events.Rule(scope, `EventBridgeRule-${args.name}`, {
    eventPattern: {
      detail: {
        bucket: { name: [args.bucket.bucketName] },
        object: { key: [args.trainingFileFullPath] }
      },
      detailType: ['Object Created'],
      source: ['aws.s3']
    },
    targets: [
      new targets.SfnStateMachine(stateMachine, {
        input: events.RuleTargetInput.fromObject({
          s3Bucket: args.bucket.bucketName,
          s3Key: args.trainingFileFullPath
        }),
        role: eventRole
      })
    ]
  });

  // copy training job output to "latest" for easy discovery by predictor service

  errorOnPathOverlap(args.modelOutputDirname, args.copyTargetDirname);

  const logGroup = new logs.LogGroup(scope, `MlModelCopyFn-${args.name}LogGroup`, {
    removalPolicy: cdk.RemovalPolicy.DESTROY,
    retention: logs.RetentionDays.ONE_WEEK
  });

  const mlModelCopyFn = new NodejsFunction(scope, `MlModelCopyFn-${args.name}`, {
    entry: path.join(__dirname, '../../lambda/ml-model-copy.ts'),
    environment: {
      FILENAME: 'model.tar.gz',
      SOURCE_PATH: args.modelOutputDirname,
      TARGET_PATH: args.copyTargetDirname
    },
    handler: 'handler',
    logGroup,
    runtime: lambda.Runtime.NODEJS_20_X,
    timeout: cdk.Duration.seconds(30)
  });

  args.bucket.grantRead(mlModelCopyFn, `${args.modelOutputDirname}*`);
  args.bucket.grantPut(mlModelCopyFn, `${args.copyTargetDirname}*`);

  new events.Rule(scope, `OnModelArtifactCreated-${args.name}`, {
    eventPattern: {
      detail: {
        bucket: { name: [args.bucket.bucketName] },
        object: { key: [{ prefix: args.modelOutputDirname }] }
      },
      detailType: ['Object Created'],
      source: ['aws.s3']
    },
    targets: [new targets.LambdaFunction(mlModelCopyFn)]
  });
}

export class DrawdownSagemakerStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: DrawdownSagemakerStackProps) {
    super(scope, id, props);

    const { appBucket, modelsLatestDirname, modelsRunsDirname, trainingDirname, xgboostImage } = props;

    const sagemakerRole = new iam.Role(this, 'SageMakerTrainingRole', {
      assumedBy: new iam.ServicePrincipal('sagemaker.amazonaws.com')
    });
    appBucket.grantRead(sagemakerRole, `${trainingDirname}/*`);
    appBucket.grantWrite(sagemakerRole, `${modelsRunsDirname}/*`);

    const eventRole = new iam.Role(this, 'EventBridgeStartSfnRole', {
      assumedBy: new iam.ServicePrincipal('events.amazonaws.com')
    });

    const trainPipelineArgs: TrainPipelineArgs[] = ['drawdown-two-weeks', 'drawdown-five-weeks'].map((name) => ({
      bucket: appBucket,
      copyTargetDirname: `${modelsLatestDirname}/${name}/`,
      modelOutputDirname: `${modelsRunsDirname}/${name}/`,
      name,
      trainingFileFullPath: `${trainingDirname}/${name}/latest.csv.gz`
    }));

    trainPipelineArgs.forEach((args) => provisionTrainingPipeline(this, args, eventRole, sagemakerRole, xgboostImage));
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
