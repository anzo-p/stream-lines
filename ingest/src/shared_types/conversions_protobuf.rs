use chrono::{DateTime, FixedOffset};
use core::option::Option;
use prost_types::Timestamp;
use rust_decimal::prelude::ToPrimitive;

use crate::error_handling::ProcessError;
use crate::protobuf::{
    CryptoQuotationMessageProto, CryptoTradeMessageProto, MoneyProto, StockQuotationMessageProto, StockTradeMessageProto,
};
use crate::shared_types::types_crypto::{CryptoQuotationMessage, CryptoTradeMessage};
use crate::shared_types::types_money::MoneyMessage;
use crate::shared_types::types_stock::{StockQuotationMessage, StockTradeMessage};

pub fn crypto_quotation_to_protobuf(msg: &CryptoQuotationMessage) -> Result<CryptoQuotationMessageProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;

    Ok(CryptoQuotationMessageProto {
        ty: msg.message_type.clone(),
        sy: msg.symbol.clone(),
        bp: Option::from(money_to_protobuf(msg.bid_price.clone())),
        bs: msg.bid_size.clone().to_f64().unwrap(),
        ap: Option::from(money_to_protobuf(msg.ask_price.clone())),
        r#as: msg.ask_size.clone().to_f64().unwrap(),
        ti: Some(timestamp),
    })
}

pub fn crypto_trade_to_protobuf(msg: &CryptoTradeMessage) -> Result<CryptoTradeMessageProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;

    Ok(CryptoTradeMessageProto {
        ty: msg.message_type.clone(),
        sy: msg.symbol.clone(),
        i: msg.trade_id.clone(),
        p: Option::from(money_to_protobuf(msg.price.clone())),
        s: msg.size.clone().to_f64().unwrap(),
        ti: Some(timestamp),
        tks: msg.tks.clone(),
    })
}

pub fn stock_quotation_to_protobuf(msg: &StockQuotationMessage) -> Result<StockQuotationMessageProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;

    Ok(StockQuotationMessageProto {
        ty: msg.message_type.clone(),
        sy: msg.symbol.clone(),
        bx: msg.bid_exchange.clone(),
        bp: Option::from(money_to_protobuf(msg.bid_price.clone())),
        bs: msg.bid_size.clone().to_f64().unwrap(),
        ax: msg.ask_exchange.clone(),
        ap: Option::from(money_to_protobuf(msg.ask_price.clone())),
        r#as: msg.ask_size.clone().to_f64().unwrap(),
        ti: Some(timestamp),
        c: msg.conditions.clone(),
        z: msg.tape.clone(),
    })
}

pub fn stock_trade_to_protobuf(msg: &StockTradeMessage) -> Result<StockTradeMessageProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;

    Ok(StockTradeMessageProto {
        ty: msg.message_type.clone(),
        sy: msg.symbol.clone(),
        i: msg.trade_id.clone(),
        x: msg.exchange.clone(),
        p: Option::from(money_to_protobuf(msg.price.clone())),
        s: msg.size.clone().to_f64().unwrap(),
        c: msg.conditions.clone(),
        ti: Some(timestamp),
        z: msg.tape.clone(),
    })
}

fn money_to_protobuf(money: MoneyMessage) -> MoneyProto {
    MoneyProto {
        u: money.units,
        n: money.nanos,
        c: money.currency,
    }
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
