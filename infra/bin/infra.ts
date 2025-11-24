#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { AppInfraStack } from '../lib/infra/main-stack';
import * as dotenv from 'dotenv';

dotenv.config();

const env = {
  account: process.env.AWS_ACCOUNT_ID,
  region: process.env.AWS_REGION
};

const app = new cdk.App();

new AppInfraStack(app, 'StreamLines', { env });
