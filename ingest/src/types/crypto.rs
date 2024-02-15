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

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_deserialize_crypto_trade_message_fractional_amount_and_lot_size() {
        let json_value = json!({
            "T":"t",
            "S":"ETH/USD", // forget USD, read as just ETH
            "i":1,
            "p":12345.67,  // the price paid in USD
            "s":0.000001,  // cryptos commonly sell in fractions, otherwise would largely not be affordable
            "t":"2021-01-01T00:00:00Z",
            "tks":""
        });

        let message: CryptoTradeMessage = serde_json::from_value(json_value).unwrap();

        assert_eq!(message.message_type, "t");
        assert_eq!(message.symbol, "ETH/USD");
        assert_eq!(message.trade_id, 1);
        assert_eq!(message.price.units, 12345);
        assert_eq!(message.price.nanos, 67 * 10 * 1_000_000);
        assert_eq!(message.price.currency, "USD");
        assert_eq!(message.size, Decimal::new(1, 6));
        assert_eq!(message.market_timestamp.to_rfc3339(), "2021-01-01T00:00:00+00:00");
    }

    #[test]
    fn test_deserialize_crypto_trade_message_integer_amount_and_lot_size() {
        let json_value = json!({
            "T":"t",
            "S":"ETH/USD",
            "i":1,
            "p":12345,
            "s":1,
            "t":"2021-01-01T00:00:00Z",
            "tks":""
        });

        let message: CryptoTradeMessage = serde_json::from_value(json_value).unwrap();

        assert_eq!(message.message_type, "t");
        assert_eq!(message.symbol, "ETH/USD");
        assert_eq!(message.trade_id, 1);
        assert_eq!(message.price.units, 12345);
        assert_eq!(message.price.nanos, 00);
        assert_eq!(message.price.currency, "USD");
        assert_eq!(message.size, Decimal::new(1, 0));
        assert_eq!(message.market_timestamp.to_rfc3339(), "2021-01-01T00:00:00+00:00");
    }
}
