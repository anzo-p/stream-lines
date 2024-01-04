use aws_sdk_kinesis::Client as KinesisClient;
use serde_json::Value;

use crate::app_config::FeedType;
use crate::error_handling::ProcessError;
use crate::shared_types::{CryptoQuotationMessage, CryptoTradeMessage, StockQuotationMessage, StockTradeMessage};
use crate::stream_producer::send_to_kinesis;
use crate::websocket::MarketMessage;

pub async fn process_item(
    feed_type: &FeedType,
    item: Value,
    kinesis_client: &KinesisClient,
) -> Result<(), ProcessError> {
    match item.get("T").and_then(Value::as_str) {
        Some("q") => process_quotation_message(feed_type, item, kinesis_client).await,
        Some("t") => process_trade_message(feed_type, item, kinesis_client).await,
        Some("success") => {
            println!("Received success message");
            Ok(())
        }
        Some("subscription") => {
            println!("Received subscription message");
            Ok(())
        }
        _ => {
            println!("Unknown message type");
            Ok(())
        }
    }
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
