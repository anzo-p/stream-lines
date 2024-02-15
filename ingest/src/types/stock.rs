use chrono::{DateTime, FixedOffset};
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};

use crate::types::money::{deserialize_money_message, MoneyMessage};

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

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_deserialize_stock_trade_message() {
        let json_value = json!({
            "T":"t",
            "S":"AAPL",
            "i":1,
            "x":"NASDAQ",
            "p":123.45,
            "s":100,
            "c":["A","B"],
            "t":"2021-01-01T00:00:00-00:00",
            "z":"A"
        });

        let message: StockTradeMessage = serde_json::from_value(json_value).unwrap();

        assert_eq!(message.message_type, "t");
        assert_eq!(message.symbol, "AAPL");
        assert_eq!(message.trade_id, 1);
        assert_eq!(message.exchange, "NASDAQ");
        assert_eq!(message.price.units, 123);
        assert_eq!(message.price.nanos, 45 * 10 * 1_000_000);
        assert_eq!(message.price.currency, "USD");
        assert_eq!(message.size, Decimal::new(100, 0));
        assert_eq!(message.conditions, vec!["A", "B"]);
        assert_eq!(message.market_timestamp.to_rfc3339(), "2021-01-01T00:00:00+00:00");
        assert_eq!(message.tape, "A");
    }

    #[test]
    fn test_deserialize_stock_trade_message_integer_price() {
        let json_value = json!({
            "T":"t",
            "S":"AAPL",
            "i":1,
            "x":"NASDAQ",
            "p":123,
            "s":100,
            "c":["A","B"],
            "t":"2021-01-01T00:00:00-00:00",
            "z":"A"
        });

        let message: StockTradeMessage = serde_json::from_value(json_value).unwrap();

        assert_eq!(message.price.units, 123);
        assert_eq!(message.price.nanos, 0);
    }
}
