use async_graphql::Request;
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
    let graphql_route = warp::post()
        .and(warp::path("graphql"))
        .and(warp::body::json())
        .and_then(move |req| graphql_handler(schema.clone(), req));

    warp::serve(graphql_route).run(socket_addr).await;
}
