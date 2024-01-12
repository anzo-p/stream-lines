#!/usr/bin/env node
import "source-map-support/register";
import * as cdk from "aws-cdk-lib";
import { DynamodbStack } from "../lib/dynamodb-stack";

const app = new cdk.App();
new DynamodbStack(app, "DynamodbStack", {});
