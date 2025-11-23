## This module provides all the cloud infrastructure required to run the Stream Lines application.

!NB some resources might fail to teardown completely. AWS itself recommends to browse through your console and delete/remove all residual components manually, lest they incur hourly based costs for mere existence. API Gateway, ECS, EC2, Kinesis, VPC..


Running CDK
```
npm install -g aws-cdk
npm install

# once
npx cdk bootstrap aws://<account>/<region> -c stack=foundations
npx cdk bootstrap aws://<account>/<region> -c stack=infra

npx cdk synth -c stack=<stack>
npx cdk deploy -c stack=<stack>
```

## 1 Provision foundational resources

`lib/foundations` lists out some of the resources that must be available when provisioning the main app infra. Pay attention that none of these resources exists inside the Vpc to be created for the App.

It might be easire to provision them manually as it will eitherway not succeed in one go. Those substacks still documents the minimal parameters required for manual provisioning.

### 1.1 File system for InfluxDB

Influx DB will be needing an EFS File System to persist its data. This data must last accross redeploys. Importantly it also contains all configuration data such as issued read and write tokens.

Note that the EFS Volume lives in another Vpc and we only create mount points to it fromt he target Vpc. Therefore all mount points to it must be removed after creation.

## 2 Provision the entire app infra

### 2.1 Lambdas

First deploy any lambdas from their root level module. This will require applicable S3 bucket to exists from foundations stack.
```
cd lambdas/<lambda dir>
make
```

### 2.2 InfluxDB tokens

Varioius services will need access to read and/or write to influx. This requires read and write tokens which are nonexistent when system is provisioned form a clean state without a pre-existing EFS.

#### The process to obtain read and write tokens (ie. how to Exec into a running AWS ESC Container)

1. Install AWS Session Manager Plugin
```
brew install --cask session-manager-plugin
```

2. Enable execute command on the target service in cdk
```
new ecs.FargateService(this, 'InfluxDbEcsService'
  {    
      cluster: ecsCluster,
      taskDefinition,
      ...
      enableExecuteCommand: true,
```

3. Run CDK Deploy without the services that will be using Influx by commenting them out (ripples, backend..). This is _not strictly necessary_ as they might either not yet make actual calls to influx or those calls wiill only fail on isufficient authorization.

4. `Exec` into the Influx AWS ESC Task container
```
aws ecs execute-command \
  --cluster <cluster-arn>  \
  --task <task-arn> \
  --container <container-name> \
  --interactive \
  --command "/bin/bash"
```

5. Issue tokens through influx CLI inside the container. Prefer separate tokens for read and write
```
influx auth create \
  --host http://localhost:8086 \
  --org <organization> \
  --read-buckets \ 
  --write-buckets \
  --description "write-token" \
  --token <from DOCKER_INFLUXDB_INIT_ADMIN_TOKEN in cdk>
```

6. Pass those tokens to the client services via ENVs, enable those services and redeploy CDK

### 2.3 CloudFront stack

Cloudfront is an experimental layer to this system which requires to deploy `Dashboard` (or at least the Client side of it) statically out of an S3 bucket. This is not currently activated but the required stack goes along with the repo for further experiments. Importantly CloudFront requires its ACM Certificate to be issued against `us-east-1` region.

## 3 Provision the backend only

A much sleeker and cost-effective version of the app would be to only provide a [Graphana OOS](https://grafana.com/oss/grafana/?plcmt=oss-nav) instance read access to the InfluxDB securely, eg. via an ALB or Bastion.
