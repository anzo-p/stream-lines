use crate::error_handling::ProcessError;
use crate::shared_types::{
    CryptoMarketDataMessage, CryptoQuotationMessage, CryptoTradeMessage, SerializeToProtobufMessage, StockMarketDataMessage,
    StockQuotationMessage, StockTradeMessage,
};

pub trait MarketMessage {
    fn get_partition_key(&self) -> String;
    fn to_protobuf_binary(&self) -> Result<Vec<u8>, ProcessError>;
}

impl MarketMessage for CryptoQuotationMessage {
    fn get_partition_key(&self) -> String {
        self.symbol.clone()
    }

    fn to_protobuf_binary(&self) -> Result<Vec<u8>, ProcessError> {
        Ok(CryptoMarketDataMessage::CryptoQuotation(self.clone()).to_protobuf_binary()?)
    }
}

impl MarketMessage for CryptoTradeMessage {
    fn get_partition_key(&self) -> String {
        self.symbol.clone()
    }

    fn to_protobuf_binary(&self) -> Result<Vec<u8>, ProcessError> {
        Ok(CryptoMarketDataMessage::CryptoTrade(self.clone()).to_protobuf_binary()?)
    }
}

impl MarketMessage for StockQuotationMessage {
    fn get_partition_key(&self) -> String {
        self.symbol.clone()
    }

    fn to_protobuf_binary(&self) -> Result<Vec<u8>, ProcessError> {
        Ok(StockMarketDataMessage::StockQuotation(self.clone()).to_protobuf_binary()?)
    }
}

impl MarketMessage for StockTradeMessage {
    fn get_partition_key(&self) -> String {
        self.symbol.clone()
    }

    fn to_protobuf_binary(&self) -> Result<Vec<u8>, ProcessError> {
        Ok(StockMarketDataMessage::StockTrade(self.clone()).to_protobuf_binary()?)
    }
}
