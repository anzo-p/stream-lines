## Stream Lines

The App literally draws an SVG Polyline onto a dashboard out of a stream of data consumed from a websocket, processed in a Flink DataStream, and then streamed onto that dashboard in a Web App. AWS infra included. Polyglot by choice.

```mermaid
%%{init: {"flowchart": {"htmlLabels": false}} }%%
flowchart LR
  alpacaWs[/"WebSocket feed"/]
  alpacaRest[/"Rest API"/]
  ingest("`**Ingest**
  Rust`")
  gather("`**Gather**
  Kotlin
  Spring Boot`")
  influxdb[(InfluxDB)]
  kinesisUpstream["AWS Kinesis"]
  kinesisDownstream["AWS Kinesis"]
  ripples("`**Ripples**
  Scala
  Apache Flink`")
  currents("`**Currents**
  Scala
  Apache Flink`")
  backend("`**Backend**
  Rust
  GraphQL`")
  apigateway["AWS API Gateway
  WebSocket
  AWS Lambda
  TypeScript"]
  dashboard["`**Dashboard**
  TypeScript
  Svelte`"]

  subgraph "Data Sources"
    alpacaWs
    alpacaRest  
  end
  alpacaWs --> ingest
  ingest --> kinesisUpstream
  kinesisUpstream --> ripples
  ripples --> influxdb
  subgraph "query path"
    influxdb --> backend
  end
  subgraph "real-time path"
    kinesisDownstream --> apigateway
  end  
  backend --> dashboard
  ripples --> kinesisDownstream
  apigateway --> dashboard

  alpacaRest --> gather
  gather --> currents
  currents --> influxdb

```

### Ingest

Consumes a websocket from an external provider. In this case market data feed from [Alpaca](https://alpaca.markets/data). This data is cached internally onto Kinesis from where it can be processed by data stream processing jobs.

### Gather

Consumes rest api from an external provider. Currently bar data for historical transactions from the same provider, Alpaca.

### Ripples

Compute any interesting values ot of the data stream provided by Ingest. These could quickly and easily become many DataStreams and even Flink Apps that feed input to each others to do further, deeper computations.

Importantly Ripples ouputs into two paths:

- InxfluxDB - for diagrams to initialize themselves upon queries
- Downstream Kinesis - for updates to diagrams without having to re-fetch full dataset.

### Currents

Compute analytical results out of data provided by Gather.

### Backend

A GraphQL server to provide data from InfluxDB.

### Dashboard

Draw SVG Polylines form the data as well as ruler guides to that data. Some amount of data normalising required.

## Todo

- serve svelte app from aws
- instead of tooltip write the info on top of graph or somewhere
- input filtering all systems that receive requests or consume external data
- tests
- metrics
- health server for ingest? but first make it work locally
- health server for flink
- logging
