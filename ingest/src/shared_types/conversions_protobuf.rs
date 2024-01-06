use chrono::{DateTime, FixedOffset};
use prost_types::Timestamp;
use rust_decimal::prelude::ToPrimitive;
use std::time::SystemTime;

use crate::error_handling::ProcessError;
use crate::protobuf::{
    market_data_proto, CryptoQuotationProto, CryptoTradeProto, CryptoTradeUnitProto, MarketDataProto, MoneyProto,
    StockQuotationProto, StockTradeProto, StockTradeUnitProto,
};
use crate::shared_types::types_crypto::{CryptoQuotationMessage, CryptoTradeMessage};
use crate::shared_types::types_money::MoneyMessage;
use crate::shared_types::types_stock::{StockQuotationMessage, StockTradeMessage};

pub fn crypto_quotation_to_protobuf(msg: &CryptoQuotationMessage) -> Result<MarketDataProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;
    let bid = create_crypto_trade_unit_proto(msg.bid_price.clone(), msg.bid_size.to_f64().unwrap())?;
    let ask = create_crypto_trade_unit_proto(msg.ask_price.clone(), msg.ask_size.to_f64().unwrap())?;

    let crypto_quotation = CryptoQuotationProto {
        symbol: msg.symbol.clone(),
        bid: Some(bid),
        ask: Some(ask),
        market_timestamp: Some(timestamp),
    };

    create_market_data_proto(market_data_proto::MessageType::Cqm(crypto_quotation))
}

pub fn crypto_trade_to_protobuf(msg: &CryptoTradeMessage) -> Result<MarketDataProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;
    let settle = create_crypto_trade_unit_proto(msg.price.clone(), msg.size.to_f64().unwrap())?;

    let crypto_trade = CryptoTradeProto {
        symbol: msg.symbol.clone(),
        trade_id: msg.trade_id.clone(),
        settle: Some(settle),
        market_timestamp: Some(timestamp),
        tks: msg.tks.clone(),
    };

    create_market_data_proto(market_data_proto::MessageType::Ctm(crypto_trade))
}

pub fn stock_quotation_to_protobuf(msg: &StockQuotationMessage) -> Result<MarketDataProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;
    let bid = create_stock_trade_unit_proto(&msg.bid_exchange, msg.bid_price.clone(), msg.bid_size.to_f64().unwrap())?;
    let ask = create_stock_trade_unit_proto(&msg.ask_exchange, msg.ask_price.clone(), msg.ask_size.to_f64().unwrap())?;

    let stock_quotation = StockQuotationProto {
        symbol: msg.symbol.clone(),
        bid: Some(bid),
        ask: Some(ask),
        conditions: msg.conditions.clone(),
        market_timestamp: Some(timestamp),
        tape: msg.tape.clone(),
    };

    create_market_data_proto(market_data_proto::MessageType::Sqm(stock_quotation))
}

pub fn stock_trade_to_protobuf(msg: &StockTradeMessage) -> Result<MarketDataProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;
    let settle = create_stock_trade_unit_proto(&msg.exchange, msg.price.clone(), msg.size.to_f64().unwrap())?;

    let stock_trade = StockTradeProto {
        symbol: msg.symbol.clone(),
        trade_id: msg.trade_id.clone(),
        settle: Some(settle),
        conditions: msg.conditions.clone(),
        market_timestamp: Some(timestamp),
        tape: msg.tape.clone(),
    };

    create_market_data_proto(market_data_proto::MessageType::Stm(stock_trade))
}

fn datetime_to_protobuf_timestamp(dt: DateTime<FixedOffset>) -> Result<Timestamp, ProcessError> {
    let seconds = dt.timestamp();
    let nanos = dt.timestamp_subsec_nanos();

    if seconds < 0 || nanos > 999_999_999 {
        return Err(ProcessError::ProtobufConversionError(
            "Invalid DateTime for Timestamp conversion".to_string(),
        ));
    }

    Ok(Timestamp {
        seconds,
        nanos: nanos as i32,
    })
}

fn money_to_protobuf(money: MoneyMessage) -> MoneyProto {
    MoneyProto {
        units: money.units,
        nanos: money.nanos,
        currency: money.currency,
    }
}

fn create_crypto_trade_unit_proto(price: MoneyMessage, lot_size: f64) -> Result<CryptoTradeUnitProto, ProcessError> {
    Ok(CryptoTradeUnitProto {
        price: Some(money_to_protobuf(price)),
        lot_size,
    })
}

fn create_stock_trade_unit_proto(
    exchange: &str,
    price: MoneyMessage,
    lot_size: f64,
) -> Result<StockTradeUnitProto, ProcessError> {
    Ok(StockTradeUnitProto {
        exchange: exchange.to_string(),
        price: Some(money_to_protobuf(price)),
        lot_size,
    })
}

fn create_market_data_proto(message: market_data_proto::MessageType) -> Result<MarketDataProto, ProcessError> {
    Ok(MarketDataProto {
        ingest_timestamp: Some(Timestamp::from(SystemTime::now())),
        message_type: Some(message),
    })
}
