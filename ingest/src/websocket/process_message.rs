use aws_sdk_kinesis::Client as KinesisClient;
use serde_json::Value;
use tokio_tungstenite::tungstenite::protocol::Message;

use crate::error_handling::ProcessError;
use crate::shared_types::traits::ToProtobuf;
use crate::shared_types::types::{QuotationMessage, TradeMessage};
use crate::shared_types::ReceivedMessage;
use crate::stream_producer::producer::send_to_kinesis;

pub async fn process_message(ws_message: Message, kinesis_client: &KinesisClient) -> Result<(), ProcessError> {
    match ws_message {
        Message::Text(text) => {
            let json_value: Value = serde_json::from_str(&text)?;
            loop_market_data(json_value, kinesis_client).await?;
            Ok(())
        }

        _ => {
            // log or error
            println!("Received non-text message");
            Ok(())
        }
    }
}

async fn process_item(item: Value, kinesis_client: &KinesisClient) -> Result<(), ProcessError> {
    match item.get("T").and_then(Value::as_str) {
        Some("q") => {
            let message: QuotationMessage = serde_json::from_value(item).map_err(|e| ProcessError::ParsingError {
                msg: e.to_string(),
                item_type: "QuotationMessage".to_string(),
            })?;

            let partition_key = message.symbol.clone();
            let data = ReceivedMessage::QuotationMessage(message).to_protobuf()?;

            send_to_kinesis(kinesis_client, &partition_key, data).await?;
        }

        Some("t") => {
            let message: TradeMessage = serde_json::from_value(item).map_err(|e| ProcessError::ParsingError {
                msg: e.to_string(),
                item_type: "TradeMessage".to_string(),
            })?;

            let partition_key = message.symbol.clone();
            let data = ReceivedMessage::TradeMessage(message).to_protobuf()?;

            send_to_kinesis(kinesis_client, &partition_key, data).await?;
        }

        Some("success") => println!("{:?}", item.get("msg")),

        Some("subscription") => println!("successful subscription to market data: {:?}", item.to_string()),

        _ => return Err(ProcessError::UnknownItemType(item.clone())),
    }

    Ok(())
}

async fn loop_market_data(values: Value, kinesis_client: &KinesisClient) -> Result<(), ProcessError> {
    if let Value::Array(array) = values {
        for item in array {
            process_item(item, kinesis_client).await?
        }
    }
    Ok(())
}
