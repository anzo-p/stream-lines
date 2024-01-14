use chrono::{DateTime, FixedOffset};
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};

use crate::types::money::{deserialize_money_message, MoneyMessage};

pub enum CryptoMarketDataMessage {
    CryptoQuotation(CryptoQuotationMessage),
    CryptoTrade(CryptoTradeMessage),
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct CryptoQuotationMessage {
    #[serde(rename = "T")]
    pub message_type: String,
    #[serde(rename = "S")]
    pub symbol: String,
    #[serde(rename = "bp", deserialize_with = "deserialize_money_message")]
    pub bid_price: MoneyMessage,
    #[serde(rename = "bs")]
    pub bid_size: Decimal,
    #[serde(rename = "ap", deserialize_with = "deserialize_money_message")]
    pub ask_price: MoneyMessage,
    #[serde(rename = "as")]
    pub ask_size: Decimal,
    #[serde(rename = "t")]
    pub market_timestamp: DateTime<FixedOffset>,
}

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct CryptoTradeMessage {
    #[serde(rename = "T")]
    pub message_type: String,
    #[serde(rename = "S")]
    pub symbol: String,
    #[serde(rename = "i")]
    pub trade_id: i64,
    #[serde(rename = "p", deserialize_with = "deserialize_money_message")]
    pub price: MoneyMessage,
    #[serde(rename = "s")]
    pub size: Decimal,
    #[serde(rename = "t")]
    pub market_timestamp: DateTime<FixedOffset>,
    // unknown for now
    pub tks: String,
}
