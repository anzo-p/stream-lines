# Infra

This module provides all cloud infrastructure required to run the App.

## 1. !NB

On stack destroy some resources might fail to teardown completely. _AWS recommends to browse through your console and delete/remove all residual components manually_, lest they incur hourly based costs for mere existence. API Gateway, ECS, EC2, Kinesis, VPC..

## 2. Foundational resources

The app needs the following resource that must live and persist outside the apps Vpc and scope.

### 2.1. Add an S3 bucket

With following structure
```
flink/
lambdas/
```

### 2.2. Required ECR repositories

These ar 'backend', 'dashboard', 'influxdb', 'ingest', 'ripples', and they need to be acuirable from CDK like so

```
ecr.Repository.fromRepositoryName(this, 'EcrRepository', '<repo-name>')
```

### 2.3. Add the following Required DynamoDB tables
<table>
  <tr>
    <th>Table or GSI</th>
    <th>Table Name</th>
    <th>Partition Key</th>
    <th>Sort Key</th>
  </tr>
  <tr>
    <td>Table</td>
    <td>StreamLinesWebsocketConnectionTable</td>
    <td>connectionId: S</td>
  </tr>
  <tr>
    <td>GSI onto ..WebsocketConnectionTable</td>
    <td>StreamLinesWebsocketGetConnectionsBySymbolIndex</td>
    <td>symbol: S</td>
</table>

### 2.4. Add an EBS drive

A drive is required to persist InfluxDB data through stack destroys and re-deploys. An EBS is much faster and guaranteed cheaper than EFS. This drive contains not only the important data of the app, but also all configuration data given to and required by the database management system.

Go to AWS Console > EC2 > Elastic Block Store > Volumes > Create Volume. `gp3` with defaults will do. Leave it unmounted. Note down the Volume ID.

### 2.5. Deploy the AWS lambdas

```
cd lambdas/<lambda dir>
make
```

## 3. Deplopy the app

Before hitting cdk dploy make sure to read section 2.2 below about InfluxDB tokens.

### 3.1. CDK deploy commands

```
npm install -g aws-cdk
npm install

# once
npx cdk bootstrap aws://<account>/<region>

npx cdk synth
npx cdk deploy
```

### 3.2. InfluxDB tokens

Varioius services will need access to read and/or write to influx. This requires read and write tokens which are nonexistent when system is provisioned for the first time (ie. form a clean EBS).

Use ssh or Session Manager to connect into the EC2 

1. In cdk enable SSM to execute command on target instance
```
   const ssmRole = new iam.Role(this, 'Ec2SsmRole', {
      assumedBy: new iam.ServicePrincipal('ec2.amazonaws.com'),
    });

    ssmRole.addManagedPolicy(
      iam.ManagedPolicy.fromAwsManagedPolicyName('AmazonSSMManagedInstanceCore'),
    );

    const instance = new ec2.Instance(this, 'InfluxDbEc2Instance', {
      ...
      role: ssmRole,
    });
```

3. Run CDK Deploy without the services that will be using Influx by commenting them out (ripples, backend..). This is _not strictly necessary_ as they might either not yet make actual calls to influx or those calls will only fail on isufficient authorization.

4. Inside the EC2 isnstance (sudo) docker exec into the influxdb container  and issue tokens

Prefer separate tokens for read and write
```
influx auth create \
  --host http://localhost:8086 \
  --org <organization> \
  --read-buckets \ 
  --write-buckets \
  --description "write-token" \
  --token <from DOCKER_INFLUXDB_INIT_ADMIN_TOKEN in cdk>
```

5. Pass those tokens to the client services via ENVs, enable those services and redeploy CDK

#### 3.2.1. Troubleshooting InfluxDB host service

Use SSM or SSH to connect into the EC2 instance hosting InfluxDB.

See the results of SSM commands of Influx stack in cdk. In this setup ssm commands are those that can access the internet, update system, install packages etc.
```
sudo tail -n 200 /var/log/amazon/ssm/amazon-ssm-agent.log
```
See the resuls of userdata commands of Influx stack in cdk. This is the normal shell and contains all the commands that do not require internet access.
```
sudo tail -n 300 /var/log/cloud-init-output.log
```

### 3.3. CloudFront stack

The stack to inlude a Cloudfront is an experimental layer to this system which requires to deploy `Dashboard` (or at least the Client side of it) statically out of an S3 bucket. This is not currently activated but the required stack goes along with the repo for further experiments. Importantly CloudFront requires its ACM Certificate to be issued against `us-east-1` region.

## 4. Alternatively provision the backend only

A much sleeker and cost-effective version of the app would be to only provide read access for the InfluxDB to a [Grafana OSS](https://grafana.com/oss/grafana/?plcmt=oss-nav) instance. This would deploy everyhting up to InfluxDB so VPC, Kinesis upstream, Ingest, Ripples, InfluxDB and a means to access the data securely, eg. via an ALB or Bastion. (Only none of the API Gateway, Lambdas, Kinesis downstream, ALBs, Backend, Dashboard, CloudFront, ACM Certifications etc. are deployed.)

### 4.1. Access InfluxDb form open internet

Generate ssh keys
```
ssh-keygen -t rsa -b 4096 -C "stream-lines-bastion" -f ~/.ssh/stream-lines-bastion
```

Make base64 formats for AWS out of the public keys

```
# Linux
base64 -w0 ~/.ssh/stream-lines-bastion.pub > ~/.ssh/stream-lines-bastion.pub.b64

# Mac
base64 < ~/.ssh/stream-lines-bastion.pub | tr -d '\n' > ~/.ssh/stream-lines-bastion.pub.b64
```

Import the public key into AWS EC2
```
aws ec2 import-key-pair \
  --key-name stream-lines-bastion \
  --public-key-material file://~/.ssh/stream-lines-bastion.pub.b64
```

Access Influx via SSH tunnel through Bastion
```
ssh -i ~/.ssh/stream-lines-bastion \
  -L 8086:<influx instance private IP>:8086 ec2-user@<bastion instance public IP>
```
