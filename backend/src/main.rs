mod errors;
mod graphql_schema;
mod graphql_server;
mod influxdb_conn;

use crate::influxdb_conn::create_influxdb_client;
use std::env;
use std::net::{IpAddr, SocketAddr};
use std::str::FromStr;

use crate::errors::ProcessError;
use crate::graphql_schema::create_schema;
use crate::graphql_server::start_server;

#[tokio::main]
async fn main() {
    let address_str = env::var("GRAPHQL_SERVER_ADDRESS").unwrap_or_else(|_| "127.0.0.1".to_string());
    let port_str = env::var("GRAPHQL_SERVER_PORT").unwrap_or_else(|_| "3030".to_string());

    let ip = IpAddr::from_str(&address_str).expect("Unable to parse IP address");
    let port = port_str.parse::<u16>().expect("Unable to parse port number");
    let socket_addr = SocketAddr::new(ip, port);

    let influx_client = create_influxdb_client()
        .await
        .expect("Failed to create InfluxDB client");
    let schema = create_schema(influx_client);

    start_server(schema, socket_addr).await;
}
