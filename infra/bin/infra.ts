#!/usr/bin/env node
import 'source-map-support/register';
import * as cdk from 'aws-cdk-lib';
import { InfraCoreStack } from '../lib/infra-core/main-stack';
import { ServicesStack } from '../lib/services/main-stack';
import * as dotenv from 'dotenv';

dotenv.config();

const env = {
  account: process.env.AWS_ACCOUNT_ID,
  region: process.env.AWS_REGION
};

const app = new cdk.App();

const infra = new InfraCoreStack(app, 'StreamLines-Infra', { env });

new ServicesStack(app, 'StreamLines-Services', {
  env,
  vpc: infra.vpc,
  securityGroups: infra.serviceSecurityGroups,
  ecsCluster: infra.ecsCluster
});
