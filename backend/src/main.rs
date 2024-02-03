//mod errors;
mod graphql_schema;
mod graphql_server;
mod influxdb_conn;

//use crate::errors::ProcessError;
use crate::graphql_schema::create_schema;
use crate::graphql_server::start_server;
use crate::influxdb_conn::create_influxdb_client;

#[tokio::main]
async fn main() {
    let influx_client = create_influxdb_client()
        .await
        .expect("Failed to create InfluxDB client");

    let schema = create_schema(influx_client);

    start_server(schema).await;
}
