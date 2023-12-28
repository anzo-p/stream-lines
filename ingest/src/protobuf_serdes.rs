use prost_types::Timestamp;

use crate::error_handling::ProcessError;
use crate::protobuf;
use crate::websocket::{QuotationMessage, TradeMessage};

fn quotation_message_to_protobuf(msg: &QuotationMessage) -> Result<protobuf::QuotationMessage, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;
    let bid_units = msg.bid_price.trunc();
    let bid_fracts = msg.ask_price.fract();
    let ask_units = msg.ask_price.trunc();
    let ask_fracts = msg.ask_price.fract();

    Ok(protobuf::QuotationMessage {
        ty: msg.message_type.clone(),
        sy: msg.symbol.clone(),
        bx: msg.bid_exchange.clone(),
        bpu: i32::try_from(bid_units).unwrap(),
        bpf: i32::try_from(bid_fracts).unwrap(),
        bs: msg.bid_size.clone(),
        ax: msg.ask_exchange.clone(),
        apu: i32::try_from(ask_units).unwrap(),
        apf: i32::try_from(ask_fracts).unwrap(),
        r#as: msg.ask_size.clone(),
        ti: Some(timestamp),
        c: msg.conditions.clone(),
        z: msg.tape.clone(),
    })
}

fn trade_message_to_protobuf(msg: &TradeMessage) -> Result<protobuf::TradeMessage, ProcessError> {
    let timestamp = datetime_to_protobuf_timestamp(msg.market_timestamp)?;
    let price_units = msg.price.trunc();
    let price_fracts = msg.price.fract();

    Ok(protobuf::TradeMessage {
        ty: msg.message_type.clone(),
        sy: msg.symbol.clone(),
        i: msg.trade_id.clone(),
        x: msg.exchange.clone(),
        pu: i32::try_from(price_units).unwrap(),
        pf: i32::try_from(price_fracts).unwrap(),
        s: msg.size.clone(),
        c: msg.conditions.clone(),
        ti: Some(timestamp),
        z: msg.tape.clone(),
    })
}

fn datetime_to_protobuf_timestamp(dt: DateTime<FixedOffset>) -> Result<Timestamp, ProcessError> {
    let seconds = dt.timestamp();
    let nanos = dt.timestamp_subsec_nanos();

    if seconds < 0 || nanos > 999_999_999 {
        return Err(ProcessError::ProtobufConversionError("Invalid DateTime for Timestamp conversion".to_string()));
    }

    Ok(Timestamp {
        seconds,
        nanos: nanos as i32,
    })
}
