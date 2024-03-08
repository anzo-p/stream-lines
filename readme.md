## Stream Lines

The App literally draws an SVG Polyline onto a dashboard out of a stream of data consumed from a websocket, processed in a Flink DataStream, and then streamed onto that dashboard in a Web App. AWS infra included. Polyglot by choice.

```mermaid
stateDiagram-v2
  [*] --> Ingest
  Ingest --> Upstream_Kinesis
  Upstream_Kinesis --> Analytics
  Analytics --> InfluxDB: query path
  InfluxDB --> Backend
  Backend --> Dashboard: GraphQL
  Analytics --> Downstream_Kinesis: real-time path
  Downstream_Kinesis --> ApiGateway
  ApiGateway --> Dashboard: WebSocket
```

### Ingest

Consumes a websocket from an external provider. In this case market data feed from [Alpaca](https://alpaca.markets/data). This data is cached internally onto Kinesis from where it can be processed by data stream processing jobs.

### Analytics

Compute any interesting values ot of that data stream. These could quickly and easily become many DataStreams and even Flink Apps that feed input to each others to do further, deeper computations.

Importantly Analytics ouputs into two places

- InxfluxDB - for diagrams to initialize themselves upon queries from this data
- Downstream Kinesis - for updates to those diagrams without the need to re-fetch full dataset all the time.

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
