use aws_sdk_kinesis::Client as KinesisClient;
use rust_decimal::prelude::ToPrimitive;
use serde_json::Value;
use std::string::String;
use tokio::time::{sleep, Duration};
use tokio_tungstenite::tungstenite::protocol::Message;

use crate::app_config::WebSocketFeed;
use crate::error_handling::{handle_process_error, ProcessError};
use crate::websocket::{acquire_connection, process_item, read_from_connection};

pub async fn run_one_feed(
    feed: WebSocketFeed,
    kinesis_client: KinesisClient,
    max_retries: usize,
) -> Result<(), ProcessError> {
    let mut retry_count = 0;

    while retry_count <= (max_retries.to_i8().unwrap() + 1) {
        if let Err(e) = handle_feed(feed.clone(), kinesis_client.clone()).await {
            eprintln!("Error in feed '{}': {:?}. Retrying...", feed.url, e);
            retry_count += 1;
            sleep(Duration::from_secs(10)).await;
        } else {
            return Ok(());
        }
    }
    Err(ProcessError::MaxRetriesReached("Max retries reached".to_string()))
}

async fn handle_feed(feed: WebSocketFeed, kinesis_client: KinesisClient) -> Result<(), ProcessError> {
    match acquire_connection(&feed.url).await {
        Ok(_) => {
            if let Err(e) = handle_websocket_stream(&feed, &kinesis_client).await {
                eprintln!("Error in handle_websocket_stream: {:?}", e);
                handle_process_error(&e);
                Err(e)
            } else {
                Ok(())
            }
        }
        Err(e) => {
            eprintln!("Error in acquire_connection: {:?}", e);
            handle_process_error(&e);
            Err(e)
        }
    }
}

async fn handle_websocket_stream(config: &WebSocketFeed, kinesis_client: &KinesisClient) -> Result<(), ProcessError> {
    loop {
        match read_from_connection(&config.url).await {
            Ok(Some(message)) => match process_message(&config, message, kinesis_client).await {
                Ok(_) => {}
                Err(e) => return Err(e),
            },
            Ok(None) => {}
            Err(e) => {
                println!("Error in read_from_connection: {:?}", e);
                return Err(e);
            }
        }
    }
}

async fn process_message(
    config: &WebSocketFeed,
    ws_message: Message,
    kinesis_client: &KinesisClient,
) -> Result<(), ProcessError> {
    match ws_message {
        Message::Text(text) => {
            match serde_json::from_str::<Value>(text.as_str()) {
                Ok(message) => {
                    if message.is_object() {
                        process_item(&config.feed_type, message, kinesis_client).await?
                    } else if message.is_array() {
                        loop_market_data(&config, message, kinesis_client).await?;
                    } else {
                        match message.as_str() {
                            Some(str) => {
                                let preview = str.chars().take(200).collect::<String>();
                                println!("Received JSON is not processable: {}", preview);
                            }
                            None => {
                                println!("Message content is None");
                            }
                        }
                    }
                }
                Err(e) => {
                    println!(
                        "{} - Received message is not in JSON format: {}",
                        chrono::Local::now(),
                        e
                    );
                }
            }
            Ok(())
        }

        _ => {
            println!(
                "{} - Received non-content message: {}",
                chrono::Local::now(),
                ws_message
            );
            Ok(())
        }
    }
}

async fn loop_market_data(
    config: &WebSocketFeed,
    values: Value,
    kinesis_client: &KinesisClient,
) -> Result<(), ProcessError> {
    if let Value::Array(messages) = values {
        for item in messages {
            process_item(&config.feed_type, item, kinesis_client).await?
        }
    }
    Ok(())
}
