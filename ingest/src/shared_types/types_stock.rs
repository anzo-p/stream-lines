use chrono::{DateTime, FixedOffset};
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};

use crate::shared_types::types_money::{deserialize_money_message, MoneyMessage};

pub enum StockMarketDataMessage {
    StockQuotation(StockQuotationMessage),
    StockTrade(StockTradeMessage),
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct StockQuotationMessage {
    #[serde(rename = "T")]
    pub message_type: String,
    #[serde(rename = "S")]
    pub symbol: String,
    #[serde(rename = "bx")]
    pub bid_exchange: String,
    #[serde(rename = "bp", deserialize_with = "deserialize_money_message")]
    pub bid_price: MoneyMessage,
    #[serde(rename = "bs")]
    pub bid_size: Decimal,
    #[serde(rename = "ax")]
    pub ask_exchange: String,
    #[serde(rename = "ap", deserialize_with = "deserialize_money_message")]
    pub ask_price: MoneyMessage,
    #[serde(rename = "as")]
    pub ask_size: Decimal,
    #[serde(rename = "t")]
    pub market_timestamp: DateTime<FixedOffset>,
    #[serde(rename = "c")]
    pub conditions: Vec<String>,
    #[serde(rename = "z")]
    pub tape: String,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct StockTradeMessage {
    #[serde(rename = "T")]
    pub message_type: String,
    #[serde(rename = "S")]
    pub symbol: String,
    #[serde(rename = "i")]
    pub trade_id: i64,
    #[serde(rename = "x")]
    pub exchange: String,
    #[serde(rename = "p", deserialize_with = "deserialize_money_message")]
    pub price: MoneyMessage,
    #[serde(rename = "s")]
    pub size: Decimal,
    #[serde(rename = "c")]
    pub conditions: Vec<String>,
    #[serde(rename = "t")]
    pub market_timestamp: DateTime<FixedOffset>,
    #[serde(rename = "z")]
    pub tape: String,
}
