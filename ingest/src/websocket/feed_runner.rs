use aws_sdk_kinesis::Client as KinesisClient;
use serde_json::Value;
use std::string::String;
use tokio_tungstenite::tungstenite::protocol::Message;

use crate::app_config::AppConfig;
use crate::app_config::WebSocketFeed;
use crate::error_handling::{handle_process_error, ProcessError};
use crate::stream_producer::create_kinesis_client;
use crate::websocket::{acquire_connection, process_item, read_from_connection};

pub async fn run_feeds(app_config: &AppConfig) -> Result<(), ProcessError> {
    let feeds = app_config.feeds.clone();
    let mut tasks = Vec::new();

    for feed in feeds {
        let kinesis_client = create_kinesis_client().await?;
        let task = tokio::spawn(handle_feed(feed, kinesis_client));
        tasks.push(task);
    }

    for task in tasks {
        let _ = task.await.expect("Task failed");
    }
    Ok(())
}

async fn handle_feed(feed: WebSocketFeed, kinesis_client: KinesisClient) -> Result<(), ProcessError> {
    match acquire_connection().await {
        Ok(_) => {
            if let Err(e) = handle_websocket_stream(&feed, &kinesis_client).await {
                handle_process_error(&e);
                Err(e)
            } else {
                Ok(())
            }
        }
        Err(e) => {
            handle_process_error(&e);
            Err(e)
        }
    }
}

async fn handle_websocket_stream(config: &WebSocketFeed, kinesis_client: &KinesisClient) -> Result<(), ProcessError> {
    while let Ok(Some(message)) = read_from_connection(&config.url).await {
        match process_message(&config, message, kinesis_client).await {
            Ok(_) => {}
            Err(e) => return Err(e),
        }
    }
    Ok(())
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
                    println!("Received messageis not JSON: {}", e);
                }
            }
            Ok(())
        }

        _ => {
            // log or error
            println!("Received non-text message");
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
