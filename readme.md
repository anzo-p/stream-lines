## Stream Lines

The App literally draws an SVG Polyline onto a dashboard out of a stream of data consumed from a websocket, processed in a Flink DataStream, and then streamed onto that dashboard in a Web App. AWS infra included. Polyglot by choice.

```mermaid
%%{init: {"flowchart": {"htmlLabels": false}} }%%
flowchart LR
  %% data sources
  alpacaWebSocket[/"Alpaca WebSocket"/]
  alpacaRestApi[/"Alpaca Rest API"/]
  datajockeyRestApi[/"DataJockey Rest API"/]

  %% persistence
  influxdbUpstream[(InfluxDB)]
  influxdbDownstream[(InfluxDB)]
  dynamodb[(DynamoDB)]

  %% streams
  kinesisUpstream["AWS Kinesis"]
  kinesisDownstream["AWS Kinesis"]

  %% AI
  sagemaker["AWS SageMaker"]

  %% services
  ingest("`**Ingest**
  Rust`")

  gather("`**Gather**
  Kotlin
  Spring Boot`")

  ripples("`**Ripples**
  Scala
  Apache Flink`")

  currents("`**Currents**
  Scala
  Apache Flink`")

  beacons("`**Beacons**
  *Planned..*
  `")
  classDef dashedBox stroke-dasharray: 5 5;
  class beacons dashedBox;

  narwhal("`**Narwhal**
  Python
  Numpy, XGBoost`")

  backend("`**Backend**
  Rust
  GraphQL`")

  %% frontend
  apigateway["AWS API Gateway
  WebSocket
  AWS Lambda
  TypeScript"]

  dashboard("`**Dashboard**
  TypeScript
  Svelte`")

  subgraph "data sources"
    alpacaWebSocket
    alpacaRestApi
    datajockeyRestApi
  end

  alpacaWebSocket --> ingest
  ingest --> kinesisUpstream
  kinesisUpstream --> ripples
  ripples --> kinesisDownstream
  ripples --> influxdbDownstream

  alpacaRestApi --> gather
  datajockeyRestApi --> gather
  gather --> influxdbUpstream
  influxdbUpstream --> currents
  currents --> influxdbDownstream

  gather --> dynamodb
  dynamodb --> beacons
  influxdbUpstream --> beacons
  beacons --> influxdbDownstream

  influxdbDownstream --> narwhal
  narwhal --> sagemaker
  sagemaker --> narwhal
  narwhal --> influxdbDownstream

  subgraph "analytics"
    ripples
    currents
    beacons
  end

  subgraph "machine learning"
    narwhal
  end

  subgraph "real-time path"
    kinesisDownstream --> apigateway
  end  

  subgraph "query path"
    influxdbDownstream --> backend
  end

  apigateway --> dashboard
  backend --> dashboard
```

### Ingest

Consumes a websocket from an external provider. In this case market data feed from [Alpaca](https://alpaca.markets/data). This data is cached internally onto Kinesis from where it can be processed by data stream processing jobs.

### Gather

Consumes rest api from an external provider. Currently bar data for historical transactions from Alpaca.

### Ripples

Compute any interesting values ot of the data stream provided by Ingest. These could quickly and easily become many DataStreams and even Flink Apps that feed input to each others to do further, deeper computations.

Importantly Ripples ouputs into two paths:

- InxfluxDB - for diagrams to initialize themselves upon queries
- Downstream Kinesis - for updates to diagrams without having to re-fetch full dataset.

### Currents

Compute analytical results out of data provided by Gather.

### Beacons (planned service)

Becons would compute analytical results from company financials themselves and also from interesting relationships between fundamentals and stock price development.

### Narwhal

Narwhal commposes learning data for AWS Sagemaker to calculate a model using XGBoost and then uses that model to run predictions.

### Backend

A GraphQL server to provide data from InfluxDB.

### Dashboard

Draw SVG Polylines form the data as well as ruler guides to that data. Some amount of data normalising required.
