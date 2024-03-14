#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { StreamLines } from '../lib/main-stack';
import * as dotenv from 'dotenv';

dotenv.config();

const app = new cdk.App();
new StreamLines(app, 'StreamLines', {
  env: {
    account: process.env.AWS_ACCOUNT,
    region: process.env.AWS_REGION
  }
});
