#!/usr/bin/env node
import "source-map-support/register";
import * as cdk from "aws-cdk-lib";
import * as dotenv from "dotenv";
import { ControlTowerStack } from "../lib/control-tower-stack";

dotenv.config();

const app = new cdk.App();
new ControlTowerStack(app, "ControlTowerStack", {});
