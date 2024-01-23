use crate::config::{FeedType, WebSocketFeed};
use aws_sdk_kinesis::Client as KinesisClient;
use serde_json::Value;

use crate::errors::ProcessError;
use crate::stream_producer::send_to_kinesis;
use crate::types::{CryptoQuotationMessage, CryptoTradeMessage, StockQuotationMessage, StockTradeMessage};
use crate::ws_feed_consumer::MarketMessage;

pub async fn process_one(
    feed_type: &FeedType,
    item: Value,
    kinesis_client: &KinesisClient,
) -> Result<(), ProcessError> {
    match item.get("T").and_then(Value::as_str) {
        Some("q") => process_quotation_message(feed_type, item, kinesis_client).await,
        Some("t") => process_trade_message(feed_type, item, kinesis_client).await,
        Some("success") => {
            match item.get("msg").and_then(Value::as_str) {
                Some("connected") => log::info!("Successfully connected to {} feed", &feed_type),
                Some("authenticated") => log::info!("Successfully authenticated to {} feed", feed_type),
                _ => log::info!("Unknown success message: {:?}", item),
            }
            Ok(())
        }
        Some("subscription") => {
            log::info!("Successfully subscribed to feed {}", feed_type);
            Ok(())
        }
        _ => {
            log::info!("Unknown message type");
            Ok(())
        }
    }
}

pub async fn process_many(
    config: &WebSocketFeed,
    values: Value,
    kinesis_client: &KinesisClient,
) -> Result<(), ProcessError> {
    if let Value::Array(messages) = values {
        for item in messages {
            process_one(&config.feed_type, item, kinesis_client).await?
        }
    }
    Ok(())
}

async fn process_quotation_message(
    feed_type: &FeedType,
    item: Value,
    kinesis_client: &KinesisClient,
) -> Result<(), ProcessError> {
    match feed_type {
        FeedType::Stocks => process_market_message::<StockQuotationMessage>(item, kinesis_client).await,
        FeedType::Crypto => process_market_message::<CryptoQuotationMessage>(item, kinesis_client).await,
    }
}

async fn process_trade_message(
    feed_type: &FeedType,
    item: Value,
    kinesis_client: &KinesisClient,
) -> Result<(), ProcessError> {
    match feed_type {
        FeedType::Stocks => process_market_message::<StockTradeMessage>(item, kinesis_client).await,
        FeedType::Crypto => process_market_message::<CryptoTradeMessage>(item, kinesis_client).await,
    }
}

async fn process_market_message<M: MarketMessage + for<'de> serde::Deserialize<'de>>(
    item: Value,
    kinesis_client: &KinesisClient,
) -> Result<(), ProcessError> {
    //eprintln!("Received message: {:?}", item);
    let message: M = serde_json::from_value(item)?;
    let partition_key = message.get_partition_key();
    let data = message.to_protobuf_binary()?;

    send_to_kinesis(kinesis_client, &partition_key, data)
        .await
        .map_err(|e| ProcessError::KinesisSendError(e.to_string()))
}
