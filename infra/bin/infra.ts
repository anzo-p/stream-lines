#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { ControlTowerStack } from '../lib/main-stack';
import * as dotenv from 'dotenv';

dotenv.config();

const app = new cdk.App();
new ControlTowerStack(app, 'ControlTowerStack', {
  env: {
    account: process.env.AWS_ACCOUNT,
    region: process.env.AWS_REGION
  }
});
