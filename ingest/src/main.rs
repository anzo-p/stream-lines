use dotenv;
use futures_util::future;

mod error_handling;
use error_handling::{handle_process_error, ProcessError};

mod websocket;
use websocket::{connect_to_feed, handle_websocket_stream};

#[tokio::main]
async fn main() {
    dotenv::dotenv().ok();

    match connect_to_feed().await {
        Ok(ws_stream) => {
            tokio::spawn(async move {
                if let Err(e) = handle_websocket_stream(ws_stream).await {
                    handle_process_error(&e);
                }
            });

            // Continue with other tasks, like launching a Kinesis consumer
            // ...
        }
        Err(e) => handle_process_error(&e),
    }

    future::pending::<()>().await;
}
