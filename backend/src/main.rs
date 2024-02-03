//mod errors;
mod config;
mod graphql_schema;
mod graphql_server;
mod influxdb_conn;

//use crate::errors::ProcessError;
use crate::config::logger;
use crate::graphql_schema::create_schema;
use crate::graphql_server::start_server;
use crate::influxdb_conn::create_influxdb_client;
use log::info;

#[tokio::main]
async fn main() {
    logger::init();
    info!("Application starting");

    let influx_client = create_influxdb_client()
        .await
        .expect("Failed to create InfluxDB client");

    info!("Influx DB discovered");

    let schema = create_schema(influx_client);

    info!("Graphql schema created");

    start_server(schema).await;
}
