use dotenv;
use futures_util::future;

mod error_handling;
mod protobuf;
mod shared_types;
mod stream_producer;
mod websocket;

use crate::stream_producer::create_kinesis_client;
use error_handling::{handle_process_error, ProcessError};
use websocket::{connect_to_stream, handle_websocket_stream};

#[tokio::main]
async fn main() {
    dotenv::dotenv().ok();

    match create_kinesis_client().await {
        Ok(kinesis_client) => match connect_to_stream().await {
            Ok(_) => {
                if let Err(e) = handle_websocket_stream(&kinesis_client).await {
                    handle_process_error(&e);
                }
            }
            Err(e) => handle_process_error(&e),
        },
        Err(e) => handle_process_error(&e),
    }

    future::pending::<()>().await;
}
