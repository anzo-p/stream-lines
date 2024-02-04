use async_graphql::Request;
use std::env;
use std::net::{IpAddr, SocketAddr};
use std::str::FromStr;
use warp::cors::Cors;
use warp::http::Response;
use warp::hyper::Body;
use warp::{Filter, Reply};

use crate::graphql_schema::MySchema;

pub async fn start_server(schema: MySchema) {
    let health_route = warp::path("health").map(|| warp::reply());

    let graphql_route = warp::post()
        .and(warp::path("graphql"))
        .and(warp::body::json())
        .and_then(move |req| graphql_handler(schema.clone(), req));

    let routes = graphql_route.with(cors()).or(health_route);

    warp::serve(routes).run(SocketAddr::new(parse_ip_from_env(), parse_port_from_env())).await;
}

fn parse_ip_from_env() -> IpAddr {
    let address_str = env::var("GRAPHQL_SERVER_ADDRESS").unwrap_or_else(|_| "127.0.0.1".to_string());
    IpAddr::from_str(&address_str).expect("Unable to parse IP address")
}

fn parse_port_from_env() -> u16 {
    let port_str = env::var("GRAPHQL_SERVER_PORT").unwrap_or_else(|_| "3030".to_string());
    port_str.parse::<u16>().expect("Unable to parse port number")
}

fn cors() -> Cors {
    warp::cors()
        .allow_any_origin()
        .allow_methods(vec!["POST"])
        .allow_headers(vec!["Content-Type", "Authorization"])
        .allow_credentials(true)
        .build()
}

async fn graphql_handler(schema: MySchema, req: Request) -> Result<Response<Body>, warp::Rejection> {
    let response = schema.execute(req).await;
    Ok(warp::reply::json(&response).into_response())
}
