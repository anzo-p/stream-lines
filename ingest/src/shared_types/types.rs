use chrono::{DateTime, FixedOffset};
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};

pub enum ReceivedMessage {
    QuotationMessage(QuotationMessage),
    TradeMessage(TradeMessage),
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
