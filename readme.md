## Stream Lines

The App literally draws an SVG Polyline onto a dashboard out of a stream of data consumed from a websocket, processed in a Flink DataStream, and then streamed onto that dashboard in a Web App. AWS infra included. Polyglot by choice.

```mermaid
%%{init: {"flowchart": {"htmlLabels": false}} }%%
flowchart LR
  alpaca[/"WebSocket feed"/]
  ingest("`**Ingest**
  Rust`")
  influxdb[(InfluxDB)]
  kinesisUpstream["AWS Kinesis"]
  kinesisDownstream["AWS Kinesis"]
  analytics("`**Analytics**
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

  alpaca --> ingest
  ingest --> kinesisUpstream
  kinesisUpstream --> analytics
    analytics --> influxdb
  subgraph "query path"
    influxdb --> backend
  end
    backend --> dashboard
  analytics --> kinesisDownstream
  subgraph "real-time path"
    kinesisDownstream --> apigateway
  end
  apigateway --> dashboard
```

### Ingest

Consumes a websocket from an external provider. In this case market data feed from [Alpaca](https://alpaca.markets/data). This data is cached internally onto Kinesis from where it can be processed by data stream processing jobs.

### Analytics

Compute any interesting values ot of that data stream. These could quickly and easily become many DataStreams and even Flink Apps that feed input to each others to do further, deeper computations.

Importantly Analytics ouputs into two paths:

- InxfluxDB - for diagrams to initialize themselves upon queries
- Downstream Kinesis - for updates to diagrams without having to re-fetch full dataset.

The term 'analytics' is misleading. Must come up with a better name. In reality any linear algorithm should do, with a little remodeling if necessary.

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
