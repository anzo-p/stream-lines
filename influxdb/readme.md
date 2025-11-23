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

```
influx auth list
influx auth create --org <organisation> --write-bucket <bucket-id>

influx bucket list
influx bucket create -o <organisation> -n <bucket>
influx query -o <organisation> 'from(bucket: "<bucket>") |> range(start: -1h)'
influx bucket delete -n <bucket>
```
