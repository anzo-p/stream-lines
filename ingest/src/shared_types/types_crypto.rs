use chrono::{DateTime, FixedOffset};
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};

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
    #[serde(rename = "bp")]
    pub bid_price: Decimal,
    #[serde(rename = "bs")]
    pub bid_size: Decimal,
    #[serde(rename = "ap")]
    pub ask_price: Decimal,
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
    #[serde(rename = "p")]
    pub price: Decimal,
    #[serde(rename = "s")]
    pub size: Decimal,
    #[serde(rename = "t")]
    pub market_timestamp: DateTime<FixedOffset>,
    // unknown for now
    pub tks: String,
}
