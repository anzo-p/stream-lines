## Installation

Push to own respository and avoid dockerhub throttling. Image contains Botocore which will also be required.

```
docker pull influxdb:2
docker build -t influxdb-botocore:2.0 .

docker run -p 8086:8086 influxdb-botocore:2.0

aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <account-no>.dkr.ecr.<aws-region>.amazonaws.com

docker tag influxdb-botocore:2.0 <account-no>.dkr.ecr.<region>.amazonaws.com/stream-lines-influxdb:2.0
docker push <account-no>.dkr.ecr.<region>.amazonaws.com/stream-lines-influxdb:2.0
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
