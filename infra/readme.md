# Infra

This module provides all cloud infrastructure required to run the App.

## 1. !NB

On stack destroy some resources might fail to teardown completely. _AWS recommends to browse through your console and delete/remove all residual components manually_, lest they incur hourly based costs for mere existence.

## 2. Foundational resources

The app needs the following resource that must live and persist outside the apps Vpc and scope.

### 2.1. Add an S3 bucket

With following structure
```
flink/
lambdas/
```

### 2.2. Required ECR repositories

These are 'backend', 'currents', 'dashboard', 'gather', 'influxdb', 'ingest', 'ripples', and they need to be acuirable from CDK like so

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
    <td>-</td>
  </tr>
  <tr>
    <td>GSI onto ..WebsocketConnectionTable</td>
    <td>StreamLinesWebsocketGetConnectionsBySymbolIndex</td>
    <td>symbol: S</td>
    <td>-</td>
  </tr>
  <tr>
    <td>Table</td>
    <td>StreamLinesCurrentsTable</td>
    <td>pk: S</td>
    <td>sk: S</td>
  </tr>
  <tr>
    <td>Table</td>
    <td>StreamLinesGatherTable</td>
    <td>pk: S</td>
    <td>sk: S</td>
  </tr>
</table>

### 2.4. Add an EBS drive

A drive is required to persist InfluxDB data through stack destroys and re-deploys. An EBS is much faster and guaranteed cheaper than EFS. Influx stores everything there, including your tokens and dashboards.

Go to AWS Console > EC2 > Elastic Block Store > Volumes > Create Volume. `gp3` with defaults will do. Leave it unmounted. Pass the volume id to the database at influx-ec2-stack.

### 2.5. Deploy the AWS lambdas

```
cd lambdas/<lambda dir>
make
```

## 3. Deplopy the app

Before hitting cdk dploy make sure to read section 2.2 below about InfluxDB tokens.

### 3.1. CDK deploy commands

```
# once
npm install -g aws-cdk
npm install
npx cdk bootstrap aws://<account>/<region>

# init provision requires SSM for services in private isolated to initialize
cdk deploy StreamLines-Infra -c enableBootstrapSsm=true
# on success remove SSM by omitting the flag
cdk deploy StreamLines-Infra

cdk deploy StreamLines-Services
cdk destroy StreamLines-Services
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

5. Pass those tokens to the client services via ENVs, enable those services and redeploy CDK.

6. **Or if you have the bastion running**, just port-forward the influx port via `ssh` and navigate to `http://localhost:8086´. Then login with the credentials given as envs at influx stack and manage the buckets and tokens in the provided _Influx Data Explorer_ UI.

#### 3.2.1. Troubleshooting InfluxDB host service

Use SSM or SSH to connect into the EC2 instance hosting InfluxDB.

- See the results of SSM commands of Influx stack in cdk. In this setup ssm commands are those that can access the internet, update system, install packages etc.
```
sudo tail -n 300 /var/log/amazon/ssm/amazon-ssm-agent.log
```
- See the resuls of userdata commands of Influx stack in cdk. This is the normal shell and contains all the commands that do not require internet access.
```
sudo tail -n 300 /var/log/cloud-init-output.log
```

### 3.3. CloudFront stack

The stack to inlude a Cloudfront is an experimental layer to this system which requires to deploy `Dashboard` (or at least the Client side of it) statically out of an S3 bucket. This is not currently activated but the required stack goes along in the monorepo for further experiments. Importantly CloudFront requires its ACM Certificate to be issued against `us-east-1` region.

## 4. Alternatively provision the backend only

A much sleeker and cost-effective version of the app would be to access the data using InfluxDB's embedded `Data Explorer UI`. (Alternatively use [Grafana OSS](https://grafana.com/oss/grafana/?plcmt=oss-nav) with a read tokens). This would deploy everyhting up to InfluxDB so VPC, Kinesis upstream, Ingest, Ripples, InfluxDB and a means to access the data securely, eg. via an ALB or Bastion. (Only none of the API Gateway, Lambdas, Kinesis downstream, ALBs, Backend, Dashboard, CloudFront, ACM Certifications etc. are deployed.)

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
INFLUX_PRIVATE_IP=<influx instance private IP>
BASTION_PUBLIC_IP=<bastion instance public IP>

ssh -i ~/.ssh/stream-lines-bastion \
  -L 8086:$INFLUX_PRIVATE_IP:8086 ec2-user@$BASTION_PUBLIC_IP
```
