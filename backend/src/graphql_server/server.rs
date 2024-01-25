use async_graphql::Request;
use std::env;
use std::net::SocketAddr;
use warp::http::Response;
use warp::hyper::Body;
use warp::{Filter, Reply};

use crate::graphql_schema::MySchema;

async fn graphql_handler(schema: MySchema, req: Request) -> Result<Response<Body>, warp::Rejection> {
    let response = schema.execute(req).await;
    Ok(warp::reply::json(&response).into_response())
}

pub async fn start_server(schema: MySchema, socket_addr: SocketAddr) {
    let origin = env::var("CORS_ORIGIN").unwrap_or_else(|_| "http://localhost:5173".to_string());

    let cors = warp::cors()
        .allow_origin(origin.as_str())
        .allow_methods(vec!["POST"])
        .allow_headers(vec!["Content-Type", "Authorization"])
        .allow_credentials(true);

    let graphql_route = warp::post()
        .and(warp::path("graphql"))
        .and(warp::body::json())
        .and_then(move |req| graphql_handler(schema.clone(), req));

    let routes = graphql_route.with(cors);

    warp::serve(routes).run(socket_addr).await;
}
