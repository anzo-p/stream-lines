## Installation

Push to own respository and avoid dockerhub throttling. Image contains Botocore which will also be required.

```
docker build -t influxdb-botocore:2.7 .

aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <account-no>.dkr.ecr.<aws-region>.amazonaws.com

docker tag influxdb-botocore:2.7 <account-no>.dkr.ecr.<region>.amazonaws.com/stream-lines-influxdb:latest

docker push <account-no>.dkr.ecr.<region>.amazonaws.com/stream-lines-influxdb:latest
```

## Kickstart fully accessible baseline db for local development

From influx CLI
```
influx setup \
  --org <organisation> \
  --bucket <bucket> \
  --username <uname> \
  --password <password> \
  --token <token> \
  --retention 0 \
  --force
```

Via docker-compose ENVs
```
services:
  influxdb:
    ..
    environment:
      - INFLUXDB_DB=<db name>
      - DOCKER_INFLUXDB_INIT_MODE=setup
      - DOCKER_INFLUXDB_INIT_USERNAME=<uname>
      - DOCKER_INFLUXDB_INIT_PASSWORD=<password> 
      - DOCKER_INFLUXDB_INIT_ORG=<organisation> 
      - DOCKER_INFLUXDB_INIT_BUCKET=<bucket>
      - DOCKER_INFLUXDB_INIT_RETENTION=0 # forever
      - DOCKER_INFLUXDB_INIT_ADMIN_TOKEN=<token>
```

## Couple helpful commands to get you going

#### Using InfluxDB CLI
```
influx auth list
influx auth create --org <organisation> --write-bucket <bucket-id>

influx bucket create --org <org> --token '<token>' --name <name> --retention <number of days>d
influx bucket list --org <org> --token '<token>'

influx query -o <organisation> 'from(bucket: "<bucket>") |> range(start: -1h)'

# delete data from a measurement - deleting over full range, max 1970-2100, removes entire measurement
influx delete --org <org> --token <token> --bucket <bucket> \
  --start YYYY-MM-DDTHH:MM:SSZ --stop YYYY-MM-DDTHH:MM:SSZ \
  --predicate '_measurement="<measurement>" AND <tag>=<value>'
```

#### Using REST API
InfluxDB can be controlled remotely via REST API as in this example that deletes data from a measurement.
```
curl -X POST \
  "http://localhost:8086/api/v2/delete?org=<organization>&bucket=<bucket>" \
  -H "Authorization: Token <token>" \
  -H "Content-Type: application/json" \
  -d '{
    "start": "YYYY-MM-DDTHH:MM:SSZ",
    "stop":  "YYYY-MM-DDTHH:MM:SSZ",
    "predicate": "_measurement=\"<measurement>\"AND ticker=<ticker>"
  }'
```
REST API Commands works also through SSH port forwarding, when connecting to InfluxDB through a Jump Bastion.  

## Access InfluxDB machine running on EC2 instance through Jump Bastion

1. Create and assigng key
```
ssh-keygen -t rsa -b 4096 -C "<key name>" -f ~/.ssh/<key file name>

base64 < ~/.ssh/<key file name>.pub | tr -d '\n' > ~/.ssh/<key file name>.pub.b64

aws ec2 import-key-pair \
  --key-name <key name> \
  --public-key-material file://~/.ssh/<key file name>.pub.b64
```

2. Add key to InfluxDB EC2 instance through CDK
```
new ec2.Instance(this, 'InfluxDbEc2Instance', {
  keyName: <key name>,
  ..
```

3. ~/.ssh/config
```
Host bastion
    HostName <BASTION_PUBLIC_IP>
    User ec2-user
    IdentityFile ~/.ssh/<bastion key name>

Host influx
    HostName <INFLUX_PRIVATE_IP>
    User ec2-user
    IdentityFile ~/.ssh/<influx key name>
    ProxyJump bastion
```

4. Connect to InfluxDB machine
```
ssh influx
```

