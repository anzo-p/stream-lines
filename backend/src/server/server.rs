use async_graphql::{EmptyMutation, EmptySubscription, Object, Request, Schema};
use warp::http::Response;
use warp::hyper::Body;
use warp::{Filter, Reply};

#[derive(Clone)]
pub struct QueryRoot;

#[Object]
impl QueryRoot {
    async fn hello_world(&self) -> String {
        "Hello, world".to_string()
    }
}

pub type MySchema = Schema<QueryRoot, EmptyMutation, EmptySubscription>;

async fn graphql_handler(schema: MySchema, req: Request) -> Result<Response<Body>, warp::Rejection> {
    let response = schema.execute(req).await;
    Ok(warp::reply::json(&response).into_response())
}

pub async fn start_server() {
    let schema: MySchema = Schema::build(QueryRoot, EmptyMutation, EmptySubscription).finish();

    let graphql_route = warp::post()
        .and(warp::path("graphql"))
        .and(warp::body::json())
        .and_then(move |req| graphql_handler(schema.clone(), req));

    warp::serve(graphql_route).run(([0, 0, 0, 0], 3030)).await;
}
