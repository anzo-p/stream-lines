use crate::config::WebSocketFeed;
use aws_sdk_kinesis::Client as KinesisClient;
use serde_json::Value;
use std::string::String;
use std::time::Instant;
use tokio::time::{sleep, Duration};
use tokio_tungstenite::tungstenite::protocol::Message;

use crate::errors::{handle_process_error, ProcessError};
use crate::stream_producer::create_kinesis_client;
use crate::ws_connection::{acquire_connection, read_from_connection};
use crate::ws_feed_consumer::message_processors::process_many;
use crate::ws_feed_consumer::process_one;

pub async fn run_one_feed(feed: WebSocketFeed, max_retries: usize) -> Result<(), ProcessError> {
    let mut retry_count = 0;

    while retry_count <= max_retries {
        let kinesis_client = match create_kinesis_client().await {
            Ok(client) => {
                log::info!("Kinesis stream discovered");
                retry_count = 0;
                client
            }
            Err(e) => {
                log::warn!("Failed to create Kinesis client: {:?}", e);
                retry_count += 1;
                sleep(Duration::from_secs(10)).await;
                continue;
            }
        };

        if let Err(e) = launch_feed(feed.clone(), kinesis_client).await {
            retry_count += 1;
            log::warn!("Error in feed '{}': {:?}. Retrying...", feed.url, e);
            sleep(Duration::from_secs(10)).await;
        } else {
            return Ok(());
        }
    }
    Err(ProcessError::MaxRetriesReached("Max retries reached".to_string()))
}

async fn launch_feed(feed: WebSocketFeed, kinesis_client: KinesisClient) -> Result<(), ProcessError> {
    match acquire_connection(&feed.url).await {
        Ok(_) => {
            if let Err(e) = consume_feed(&feed, &kinesis_client).await {
                log::warn!("Error in handle_websocket_stream: {:?}", e);
                handle_process_error(&e);
                Err(e)
            } else {
                Ok(())
            }
        }
        Err(e) => {
            log::warn!("Error in acquire_connection: {:?}", e);
            handle_process_error(&e);
            Err(e)
        }
    }
}

async fn consume_feed(config: &WebSocketFeed, kinesis_client: &KinesisClient) -> Result<(), ProcessError> {
    let mut counter = 0;
    let mut tally_time = Instant::now() + Duration::from_secs(60);
    loop {
        match read_from_connection(&config.url).await {
            Ok(Some(message)) => match process_message(&config, message, kinesis_client).await {
                Ok(_) => {
                    counter += 1;
                    let current_time = Instant::now();
                    if current_time >= tally_time {
                        log::info!("{} events processed in the last minute.", counter);
                        tally_time = current_time + Duration::from_secs(60);
                        counter = 0;
                    }
                }
                Err(e) => return Err(e),
            },
            Ok(None) => {}
            Err(e) => {
                log::warn!("Error in read_from_connection: {:?}", e);
                return Err(e);
            }
        }
        sleep(Duration::from_millis(1000 / &config.max_reads_per_sec)).await;
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
                        process_one(&config.feed_type, message, kinesis_client).await?
                    } else if message.is_array() {
                        process_many(&config, message, kinesis_client).await?;
                    } else {
                        match message.as_str() {
                            Some(str) => {
                                let preview = str.chars().take(200).collect::<String>();
                                log::warn!("Received JSON is not processable: {}", preview);
                            }
                            None => {
                                log::info!("Message content is None");
                            }
                        }
                    }
                }
                Err(e) => {
                    log::warn!(
                        "{} - Received message is not in JSON format: {}",
                        chrono::Local::now(),
                        e
                    );
                }
            }
            Ok(())
        }

        _ => {
            log::info!(
                "{} - Received non-content message: {}",
                chrono::Local::now(),
                ws_message
            );
            Ok(())
        }
    }
}
