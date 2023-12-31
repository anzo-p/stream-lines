use chrono::{DateTime, FixedOffset};
use prost_types::Timestamp;
use rust_decimal::prelude::ToPrimitive;
use std::i64;

use crate::error_handling::ProcessError;
use crate::protobuf::{CryptoQuotationMessageProto, CryptoTradeMessageProto, StockQuotationMessageProto, StockTradeMessageProto};
use crate::shared_types::types_crypto::{CryptoQuotationMessage, CryptoTradeMessage};
use crate::shared_types::types_stock::{StockQuotationMessage, StockTradeMessage};

pub fn crypto_quotation_to_protobuf(msg: &CryptoQuotationMessage) -> Result<CryptoQuotationMessageProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;
    let bid_units = msg.bid_price.trunc();
    let bid_fracts = msg.ask_price.fract();
    let ask_units = msg.ask_price.trunc();
    let ask_fracts = msg.ask_price.fract();

    Ok(CryptoQuotationMessageProto {
        ty: msg.message_type.clone(),
        sy: msg.symbol.clone(),
        bpu: i64::try_from(bid_units).unwrap(),
        bpf: i64::try_from(bid_fracts).unwrap(),
        bs: msg.bid_size.clone().to_f64().unwrap(),
        apu: i64::try_from(ask_units).unwrap(),
        apf: i64::try_from(ask_fracts).unwrap(),
        r#as: msg.ask_size.clone().to_f64().unwrap(),
        ti: Some(timestamp),
    })
}

pub fn crypto_trade_to_protobuf(msg: &CryptoTradeMessage) -> Result<CryptoTradeMessageProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;
    let price_units = msg.price.trunc();
    let price_fracts = msg.price.fract();

    Ok(CryptoTradeMessageProto {
        ty: msg.message_type.clone(),
        sy: msg.symbol.clone(),
        i: msg.trade_id.clone(),
        pu: i64::try_from(price_units).unwrap(),
        pf: i64::try_from(price_fracts).unwrap(),
        s: msg.size.clone().to_f64().unwrap(),
        ti: Some(timestamp),
        tks: msg.tks.clone(),
    })
}

pub fn stock_quotation_to_protobuf(msg: &StockQuotationMessage) -> Result<StockQuotationMessageProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;
    let bid_units = msg.bid_price.trunc();
    let bid_fracts = msg.ask_price.fract();
    let ask_units = msg.ask_price.trunc();
    let ask_fracts = msg.ask_price.fract();

    Ok(StockQuotationMessageProto {
        ty: msg.message_type.clone(),
        sy: msg.symbol.clone(),
        bx: msg.bid_exchange.clone(),
        bpu: i64::try_from(bid_units).unwrap(),
        bpf: i64::try_from(bid_fracts).unwrap(),
        bs: msg.bid_size.clone().to_f64().unwrap(),
        ax: msg.ask_exchange.clone(),
        apu: i64::try_from(ask_units).unwrap(),
        apf: i64::try_from(ask_fracts).unwrap(),
        r#as: msg.ask_size.clone().to_f64().unwrap(),
        ti: Some(timestamp),
        c: msg.conditions.clone(),
        z: msg.tape.clone(),
    })
}

pub fn stock_trade_to_protobuf(msg: &StockTradeMessage) -> Result<StockTradeMessageProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;
    let price_units = msg.price.trunc();
    let price_fracts = msg.price.fract();

    Ok(StockTradeMessageProto {
        ty: msg.message_type.clone(),
        sy: msg.symbol.clone(),
        i: msg.trade_id.clone(),
        x: msg.exchange.clone(),
        pu: i64::try_from(price_units).unwrap(),
        pf: i64::try_from(price_fracts).unwrap(),
        s: msg.size.clone().to_f64().unwrap(),
        c: msg.conditions.clone(),
        ti: Some(timestamp),
        z: msg.tape.clone(),
    })
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
