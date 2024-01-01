use chrono::{DateTime, FixedOffset};
use core::option::Option;
use prost_types::Timestamp;
use rust_decimal::prelude::ToPrimitive;

use crate::error_handling::ProcessError;
use crate::protobuf::MoneyMessageProto;
use crate::protobuf::market_data::{
    market_data_message_proto, CryptoQuotationMessageProto, CryptoTradeMessageProto, StockQuotationMessageProto, StockTradeMessageProto,
};
use crate::protobuf::MarketDataMessageProto;
use crate::shared_types::types_crypto::{CryptoQuotationMessage, CryptoTradeMessage};
use crate::shared_types::types_money::MoneyMessage;
use crate::shared_types::types_stock::{StockQuotationMessage, StockTradeMessage};

pub fn crypto_quotation_to_protobuf(msg: &CryptoQuotationMessage) -> Result<MarketDataMessageProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;

    let crypto_quotation = CryptoQuotationMessageProto {
        ty: msg.message_type.clone(),
        sy: msg.symbol.clone(),
        bp: Option::from(money_to_protobuf(msg.bid_price.clone())),
        bs: msg.bid_size.clone().to_f64().unwrap(),
        ap: Option::from(money_to_protobuf(msg.ask_price.clone())),
        r#as: msg.ask_size.clone().to_f64().unwrap(),
        ti: Some(timestamp),
    };

    Ok(MarketDataMessageProto {
        message_type: Some(market_data_message_proto::MessageType::Cqm(crypto_quotation)),
    })
}

pub fn crypto_trade_to_protobuf(msg: &CryptoTradeMessage) -> Result<MarketDataMessageProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;

    let crypto_trade = CryptoTradeMessageProto {
        ty: msg.message_type.clone(),
        sy: msg.symbol.clone(),
        i: msg.trade_id.clone(),
        p: Some(money_to_protobuf(msg.price.clone()).into()),
        s: msg.size.to_f64().unwrap(),
        ti: Some(timestamp),
        tks: msg.tks.clone(),
    };

    Ok(MarketDataMessageProto {
        message_type: Some(market_data_message_proto::MessageType::Ctm(crypto_trade)),
    })
}
pub fn stock_quotation_to_protobuf(msg: &StockQuotationMessage) -> Result<MarketDataMessageProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;

    let stock_quotation = StockQuotationMessageProto {
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
    };

    Ok(MarketDataMessageProto {
        message_type: Some(market_data_message_proto::MessageType::Sqm(stock_quotation)),
    })
}

pub fn stock_trade_to_protobuf(msg: &StockTradeMessage) -> Result<MarketDataMessageProto, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;

    let stock_trade = StockTradeMessageProto {
        ty: msg.message_type.clone(),
        sy: msg.symbol.clone(),
        i: msg.trade_id.clone(),
        x: msg.exchange.clone(),
        p: Option::from(money_to_protobuf(msg.price.clone())),
        s: msg.size.clone().to_f64().unwrap(),
        c: msg.conditions.clone(),
        ti: Some(timestamp),
        z: msg.tape.clone(),
    };

    Ok(MarketDataMessageProto {
        message_type: Some(market_data_message_proto::MessageType::Stm(stock_trade)),
    })
}

fn money_to_protobuf(money: MoneyMessage) -> MoneyMessageProto {
    MoneyMessageProto {
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
