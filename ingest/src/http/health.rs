use std::env;
use std::net::{IpAddr, SocketAddr};
use std::str::FromStr;
use warp::Filter;

pub async fn launch_health_server() {
    let address_str = env::var("HEALTH_SERVER_ADDR").unwrap_or_else(|_| "127.0.0.1".to_string());
    let port_str = env::var("HEALTH_SERVER_PORT").unwrap_or_else(|_| "8080".to_string());

    let ip = IpAddr::from_str(&address_str).expect("Unable to parse IP address");
    let port = port_str.parse::<u16>().expect("Unable to parse port number");
    let socket_addr = SocketAddr::new(ip, port);

    let health_check =
        warp::path!("health").map(|| warp::reply::with_status(warp::reply::html(""), warp::http::StatusCode::OK));

    warp::serve(health_check).run(socket_addr).await;

    log::info!("Health server is running on {}", socket_addr);
}
