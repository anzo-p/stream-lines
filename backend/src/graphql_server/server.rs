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

    warp::serve(routes)
        .run(SocketAddr::new(parse_ip_from_env(), parse_port_from_env()))
        .await;
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
    let allowed_origins_str = env::var("CORS_ALLOWED_ORIGINS").expect("Unable to parse allowed origins");

    let allowed_origins: Vec<String> = allowed_origins_str.split(',').map(|s| s.trim().to_string()).collect();

    warp::cors()
        .allow_origins(allowed_origins.iter().map(|s| s.as_str()))
        .allow_methods(vec!["POST"])
        .allow_headers(vec!["Content-Type", "Authorization"])
        .allow_credentials(true)
        .build()
}

async fn graphql_handler(schema: MySchema, req: Request) -> Result<Response<Body>, warp::Rejection> {
    let response = schema.execute(req).await;
    Ok(warp::reply::json(&response).into_response())
}
