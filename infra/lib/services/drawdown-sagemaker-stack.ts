import * as cdk from 'aws-cdk-lib';
import * as events from 'aws-cdk-lib/aws-events';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as sfn from 'aws-cdk-lib/aws-stepfunctions';
import * as s3 from 'aws-cdk-lib/aws-s3';
import * as targets from 'aws-cdk-lib/aws-events-targets';
import * as tasks from 'aws-cdk-lib/aws-stepfunctions-tasks';
import { Construct } from 'constructs';

export class DrawdownSagemakerStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props?: cdk.NestedStackProps) {
    super(scope, id, props);

    const sagemakerRole = new iam.Role(this, 'SageMakerTrainingRole', {
      assumedBy: new iam.ServicePrincipal('sagemaker.amazonaws.com')
    });

    const bucketName = process.env.S3_APP_BUCKET!;
    const dataBucket = s3.Bucket.fromBucketName(this, 'AppBucket', bucketName);
    const trainDataFileName = `${process.env.NARWHAL_TRAINING_DATA_PREFIX}/train_data_latest.csv.gz`;

    sagemakerRole.addToPolicy(
      new iam.PolicyStatement({
        actions: ['s3:GetObject', 's3:ListBucket', 's3:PutObject'],
        resources: [`arn:aws:s3:::${bucketName}`, `arn:aws:s3:::${bucketName}/*`]
      })
    );

    const trainJob = new tasks.SageMakerCreateTrainingJob(this, 'TrainXGB', {
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
              s3Location: tasks.S3Location.fromBucket(dataBucket, trainDataFileName)
            }
          }
        }
      ],
      outputDataConfig: {
        s3OutputLocation: tasks.S3Location.fromBucket(dataBucket, process.env.NARWHAL_MODELS_PREFIX!)
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

    const stateMachine = new sfn.StateMachine(this, 'DrawdownSagemakerStateMachine', {
      definitionBody: sfn.DefinitionBody.fromChainable(trainJob.next(new sfn.Succeed(this, 'TrainingComplete')))
    });

    const eventRole = new iam.Role(this, 'EventBridgeStartSfnRole', {
      assumedBy: new iam.ServicePrincipal('events.amazonaws.com')
    });
    eventRole.addToPolicy(
      new iam.PolicyStatement({
        actions: ['states:StartExecution'],
        resources: [stateMachine.stateMachineArn]
      })
    );

    // requires <app bucket> -> props -> Amazon EventBridge -> Send notifs to Amazon EventBridge -> On
    new events.Rule(this, 'RunTrainingOnLatestDataUpdate', {
      eventPattern: {
        detail: {
          bucket: { name: [process.env.S3_APP_BUCKET!] },
          object: { key: [trainDataFileName] }
        },
        detailType: ['Object Created'],
        source: ['aws.s3']
      },
      targets: [
        new targets.SfnStateMachine(stateMachine, {
          input: events.RuleTargetInput.fromObject({
            s3Bucket: process.env.S3_APP_BUCKET!,
            s3Key: trainDataFileName
          }),
          role: eventRole
        })
      ]
    });
  }
}
