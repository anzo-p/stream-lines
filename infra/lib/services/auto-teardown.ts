import * as cdk from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as path from 'path';
import * as scheduler from 'aws-cdk-lib/aws-scheduler';
import * as targets from 'aws-cdk-lib/aws-scheduler-targets';
import { NodejsFunction } from 'aws-cdk-lib/aws-lambda-nodejs';
import { Construct } from 'constructs';

export interface AutoTeardownStackProps extends cdk.NestedStackProps {
  targetStackArn: string;
  targetStackName: string;
}

const teardownSchedule = scheduler.ScheduleExpression.cron({
  hour: '17', // NySe regular hours closing +1h
  minute: '00',
  timeZone: cdk.TimeZone.AMERICA_NEW_YORK
});

export class AutoTeardownStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: AutoTeardownStackProps) {
    super(scope, id, props);

    const logGroup = new logs.LogGroup(this, 'TeardownFnLogGroup', {
      removalPolicy: cdk.RemovalPolicy.DESTROY,
      retention: logs.RetentionDays.ONE_WEEK
    });

    const teardownFn = new NodejsFunction(this, 'TeardownFn', {
      description: `Automatically tears down entire stack ${props.targetStackName} after NySe has closd for today.`,
      entry: path.join(__dirname, '../../lambda/teardown.ts'),
      handler: 'handler',
      logGroup,
      runtime: lambda.Runtime.NODEJS_20_X,
      timeout: cdk.Duration.seconds(30)
    });

    teardownFn.addToRolePolicy(
      new iam.PolicyStatement({
        actions: ['cloudformation:DeleteStack', 'cloudformation:DescribeStacks'],
        resources: [props.targetStackArn]
      })
    );

    teardownFn.role?.addManagedPolicy(
      iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
    );

    const invokeRole = new iam.Role(this, 'SchedulerInvokeRole', {
      assumedBy: new iam.ServicePrincipal('scheduler.amazonaws.com')
    });
    teardownFn.grantInvoke(invokeRole);

    new scheduler.Schedule(this, 'TeardownSchedule', {
      schedule: teardownSchedule,
      target: new targets.LambdaInvoke(teardownFn, {
        input: scheduler.ScheduleTargetInput.fromObject({
          stackName: props.targetStackName
        })
      })
    });

    cdk.Tags.of(this).add('autoTeardown', 'true');
  }
}
