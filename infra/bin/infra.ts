#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { AppInfraStack } from '../lib/app-infra/main-stack';
import { FoundationsStack } from '../lib/foundations/main-stack';
import * as dotenv from 'dotenv';

dotenv.config();

const env = {
  account: process.env.AWS_ACCOUNT,
  region: process.env.AWS_REGION
};

const app = new cdk.App();

const targetStack = app.node.tryGetContext('stack');

switch (targetStack) {
  case 'foundations':
    new FoundationsStack(app, 'StreamLinesFoundations', { env });
    break;

  case 'infra':
    new AppInfraStack(app, 'StreamLines', { env });
    break;

  default:
    console.error(`Error: Unknown stack name '${targetStack}'`);
    process.exit(1);
}
