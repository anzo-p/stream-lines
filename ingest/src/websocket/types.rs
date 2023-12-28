use crate::error_handling::ProcessError;
use chrono::{DateTime, FixedOffset};
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};
use std::env;

#[derive(Serialize, Deserialize)]
pub struct AuthMessage {
    action: String,
    key: String,
    secret: String,
}

impl AuthMessage {
    pub fn new() -> Result<Self, ProcessError> {
        let key = env::var("API_KEY").map_err(|_| {
            ProcessError::EnvVarError(String::from("API_KEY not found in environment"))
        })?;
        let secret = env::var("API_SECRET").map_err(|_| {
            ProcessError::EnvVarError(String::from("API_SECRET not found in environment"))
        })?;

        Ok(AuthMessage {
            action: "auth".to_string(),
            key,
            secret,
        })
    }
}

#[derive(Serialize, Deserialize)]
pub struct SubMessage {
    action: String,
    trades: Vec<String>,
    quotes: Vec<String>,
}

impl SubMessage {
    pub fn new(tickers: Vec<String>) -> Result<Self, ProcessError> {
        Ok(SubMessage {
            action: "subscribe".to_string(),
            trades: tickers.clone(),
            quotes: tickers.clone(),
        })
    }
}

#[derive(Serialize, Deserialize, Debug)]
pub struct QuotationMessage {
    #[serde(rename = "T")]
    pub message_type: String,
    #[serde(rename = "S")]
    pub symbol: String,
    #[serde(rename = "bx")]
    pub bid_exchange: String,
    #[serde(rename = "bp")]
    pub bid_price: Decimal,
    #[serde(rename = "bs")]
    pub bid_size: i32,
    #[serde(rename = "ax")]
    pub ask_exchange: String,
    #[serde(rename = "ap")]
    pub ask_price: Decimal,
    #[serde(rename = "as")]
    pub ask_size: i32,
    #[serde(rename = "t")]
    pub market_timestamp: DateTime<FixedOffset>,
    #[serde(rename = "c")]
    pub conditions: Vec<String>,
    #[serde(rename = "z")]
    pub tape: String,
}

#[derive(Serialize, Deserialize, Debug)]
pub struct TradeMessage {
    #[serde(rename = "T")]
    pub message_type: String,
    #[serde(rename = "S")]
    pub symbol: String,
    #[serde(rename = "i")]
    pub trade_id: i32,
    #[serde(rename = "x")]
    pub exchange: String,
    #[serde(rename = "p")]
    pub price: Decimal,
    #[serde(rename = "s")]
    pub size: i32,
    #[serde(rename = "c")]
    pub conditions: Vec<String>,
    #[serde(rename = "t")]
    pub market_timestamp: DateTime<FixedOffset>,
    #[serde(rename = "z")]
    pub tape: String,
}
