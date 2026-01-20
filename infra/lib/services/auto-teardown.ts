import * as cdk from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as path from 'path';
import * as scheduler from 'aws-cdk-lib/aws-scheduler';
import { NodejsFunction } from 'aws-cdk-lib/aws-lambda-nodejs';
import { Construct } from 'constructs';

export interface AutoTeardownStackProps extends cdk.NestedStackProps {
  targetStackArn: string;
  targetStackName: string;
}

export class AutoTeardownStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: AutoTeardownStackProps) {
    super(scope, id, props);

    const teardownFn = new NodejsFunction(this, 'TeardownFn', {
      entry: path.join(__dirname, '../../lambda/teardown.ts'),
      handler: 'handler',
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

    new scheduler.CfnSchedule(this, 'TeardownSchedule', {
      flexibleTimeWindow: { mode: 'OFF' },
      scheduleExpression: 'cron(0 17 ? * * *)', // NySe securities regular hours closing +1h
      scheduleExpressionTimezone: 'America/New_York',
      target: {
        arn: teardownFn.functionArn,
        roleArn: invokeRole.roleArn,
        input: JSON.stringify({
          stackName: props.targetStackName
        })
      }
    });

    cdk.Tags.of(this).add('autoTeardown', 'true');
  }
}
