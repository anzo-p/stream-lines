import * as cdk from 'aws-cdk-lib';
import * as iam from 'aws-cdk-lib/aws-iam';
import * as lambda from 'aws-cdk-lib/aws-lambda';
import * as logs from 'aws-cdk-lib/aws-logs';
import * as path from 'path';
import * as scheduler from 'aws-cdk-lib/aws-scheduler';
import { NodejsFunction } from 'aws-cdk-lib/aws-lambda-nodejs';
import { Construct } from 'constructs';

export interface AutoShutdownEcsStackProps extends cdk.NestedStackProps {
  targetStackArn?: string;
  targetStackName?: string;
}

export class AutoShutdownEcsStack extends cdk.NestedStack {
  constructor(scope: Construct, id: string, props: AutoShutdownEcsStackProps) {
    super(scope, id, props);

    const shutdownEcsFn = new NodejsFunction(this, 'ShutdownEcsFn', {
      entry: path.join(__dirname, '../../lambda/shutdown.ts'),
      handler: 'handler',
      logRetention: logs.RetentionDays.ONE_WEEK,
      memorySize: 256,
      runtime: lambda.Runtime.NODEJS_20_X,
      timeout: cdk.Duration.seconds(60)
    });

    const { account, region } = cdk.Stack.of(this);

    shutdownEcsFn.addToRolePolicy(
      new iam.PolicyStatement({
        actions: ['ecs:ListClusters'],
        resources: ['*']
      })
    );

    shutdownEcsFn.addToRolePolicy(
      new iam.PolicyStatement({
        actions: ['ecs:ListServices', 'ecs:DescribeServices', 'ecs:UpdateService'],
        resources: [`arn:aws:ecs:${region}:${account}:service/*/*`]
      })
    );

    shutdownEcsFn.role?.addManagedPolicy(
      iam.ManagedPolicy.fromAwsManagedPolicyName('service-role/AWSLambdaBasicExecutionRole')
    );

    const invokeRole = new iam.Role(this, 'SchedulerInvokeRole', {
      assumedBy: new iam.ServicePrincipal('scheduler.amazonaws.com')
    });
    shutdownEcsFn.grantInvoke(invokeRole);

    new scheduler.CfnSchedule(this, 'ShutdownEcsSchedule', {
      flexibleTimeWindow: { mode: 'OFF' },
      scheduleExpression: 'cron(0 17 ? * * *)', // NySe securities regular hours closing +1h
      scheduleExpressionTimezone: 'America/New_York',
      target: {
        arn: shutdownEcsFn.functionArn,
        roleArn: invokeRole.roleArn
      }
    });

    cdk.Tags.of(this).add('autoShutdownEcs', 'true');
  }
}
