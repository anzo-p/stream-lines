### Push to own registry (no dockerhub throttling)

```
aws ecr get-login-password --region <region> | docker login --username AWS --password-stdin <account-no>.dkr.ecr.<aws-region>.amazonaws.com

docker build -t influxdb-botocore:2.0 .
docker push <account-no>.dkr.ecr.<region>.amazonaws.com/<repository>:2.0
docker tag influxdb-botocore:2.0 <account-no>.dkr.ecr.<region>.amazonaws.com/<repository>:2.0
```

### Helpful commands to get you going

```
influx bucket list
influx bucket delete -n <bucket>
influx bucket create -o <organisation> -n <bucket>
influx query -o <organisation> 'from(bucket: "<bucket>") |> range(start: -1h)'

influx auth list
influx auth create --org <organisation> --write-bucket <bucket-id>
```
