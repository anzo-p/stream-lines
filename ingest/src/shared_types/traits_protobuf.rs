use prost::Message;

use crate::error_handling::ProcessError;
use crate::protobuf::crypto_quotation::CryptoQuotationMessageProto;
use crate::protobuf::crypto_trade::CryptoTradeMessageProto;
use crate::protobuf::stock_quotation::StockQuotationMessageProto;
use crate::protobuf::stock_trade::StockTradeMessageProto;
use crate::shared_types::conversions_protobuf::{
    crypto_quotation_to_protobuf, crypto_trade_to_protobuf, stock_quotation_to_protobuf, stock_trade_to_protobuf,
};
use crate::shared_types::types_crypto::{CryptoMarketDataMessage, CryptoQuotationMessage, CryptoTradeMessage};
use crate::shared_types::types_stock::{StockMarketDataMessage, StockQuotationMessage, StockTradeMessage};

pub trait SerializeToProtobufMessage {
    fn to_protobuf_binary(&self) -> Result<Vec<u8>, ProcessError>;
}

trait ConvertToProtobufType {
    type ProtobufType;
    fn to_protobuf_type(&self) -> Result<Self::ProtobufType, ProcessError>;
}

impl SerializeToProtobufMessage for CryptoMarketDataMessage {
    fn to_protobuf_binary(&self) -> Result<Vec<u8>, ProcessError> {
        match self {
            CryptoMarketDataMessage::CryptoQuotation(data) => data.to_protobuf_binary(),
            CryptoMarketDataMessage::CryptoTrade(data) => data.to_protobuf_binary(),
        }
    }
}

impl SerializeToProtobufMessage for StockMarketDataMessage {
    fn to_protobuf_binary(&self) -> Result<Vec<u8>, ProcessError> {
        match self {
            StockMarketDataMessage::StockQuotation(data) => data.to_protobuf_binary(),
            StockMarketDataMessage::StockTrade(data) => data.to_protobuf_binary(),
        }
    }
}

impl SerializeToProtobufMessage for CryptoQuotationMessage {
    fn to_protobuf_binary(&self) -> Result<Vec<u8>, ProcessError> {
        let protobuf_msg = self.to_protobuf_type()?;
        let mut buf = Vec::new();
        protobuf_msg.encode(&mut buf)?;
        Ok(buf)
    }
}

impl SerializeToProtobufMessage for CryptoTradeMessage {
    fn to_protobuf_binary(&self) -> Result<Vec<u8>, ProcessError> {
        let protobuf_msg = self.to_protobuf_type()?;
        let mut buf = Vec::new();
        protobuf_msg.encode(&mut buf)?;
        Ok(buf)
    }
}

impl SerializeToProtobufMessage for StockQuotationMessage {
    fn to_protobuf_binary(&self) -> Result<Vec<u8>, ProcessError> {
        let protobuf_msg = self.to_protobuf_type()?;
        let mut buf = Vec::new();
        protobuf_msg.encode(&mut buf)?;
        Ok(buf)
    }
}

impl SerializeToProtobufMessage for StockTradeMessage {
    fn to_protobuf_binary(&self) -> Result<Vec<u8>, ProcessError> {
        let protobuf_msg = self.to_protobuf_type()?;
        let mut buf = Vec::new();
        protobuf_msg.encode(&mut buf)?;
        Ok(buf)
    }
}

impl ConvertToProtobufType for CryptoQuotationMessage {
    type ProtobufType = CryptoQuotationMessageProto;

    fn to_protobuf_type(&self) -> Result<Self::ProtobufType, ProcessError> {
        crypto_quotation_to_protobuf(self)
    }
}

impl ConvertToProtobufType for CryptoTradeMessage {
    type ProtobufType = CryptoTradeMessageProto;

    fn to_protobuf_type(&self) -> Result<Self::ProtobufType, ProcessError> {
        crypto_trade_to_protobuf(self)
    }
}

impl ConvertToProtobufType for StockQuotationMessage {
    type ProtobufType = StockQuotationMessageProto;

    fn to_protobuf_type(&self) -> Result<Self::ProtobufType, ProcessError> {
        stock_quotation_to_protobuf(self)
    }
}

impl ConvertToProtobufType for StockTradeMessage {
    type ProtobufType = StockTradeMessageProto;

    fn to_protobuf_type(&self) -> Result<Self::ProtobufType, ProcessError> {
        stock_trade_to_protobuf(self)
    }
}
