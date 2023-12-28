use serde_json::Value;
use tokio_tungstenite::tungstenite::protocol::Message;

use crate::error_handling::ProcessError;
use crate::websocket::{QuotationMessage, TradeMessage};

pub async fn process_message(ws_message: Message) -> Result<(), ProcessError> {
    match ws_message {
        Message::Text(text) => {
            let json_value: Value = serde_json::from_str(&text)?;
            loop_market_data(json_value).await?;
            Ok(())
        }

        _ => {
            // log or error
            println!("Received non-text message");
            Ok(())
        }
    }
}

async fn process_item(item: Value) -> Result<(), ProcessError> {
    match item.get("T").and_then(Value::as_str) {
        Some("q") => {
            let quotation_message: QuotationMessage = serde_json::from_value(item)
                .map_err(|e| ProcessError::ParsingError {
                    msg: e.to_string(),
                    item_type: "QuotationMessage".to_string()
                })?;

            // kinesis write
            println!("Quotation: {:?}", quotation_message);
        },

        Some("t") => {
            let trade_message: TradeMessage = serde_json::from_value(item)
                .map_err(|e| ProcessError::ParsingError {
                    msg: e.to_string(),
                    item_type: "TradeMessage".to_string()
                })?;

            // kinesis write
            println!("Trade: {:?}", trade_message);
        },

        // normal operation log
        Some("success") => println!("{:?}", item.get("msg")),

        Some("subscription") => println!("successful subscription to market data: {:?}", item.to_string()),

        _ => return Err(ProcessError::UnknownItemType(item.clone())),
    }

    Ok(())
}

async fn loop_market_data(values: Value) -> Result<(), ProcessError> {
    if let Value::Array(array) = values {
        for item in array {
            process_item(item).await?
        }
    }
    Ok(())
}
