#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct MoneyProto {
    #[prost(int64, tag = "1")]
    pub units: i64,
    #[prost(int32, tag = "2")]
    pub nanos: i32,
    #[prost(string, tag = "3")]
    pub currency: ::prost::alloc::string::String,
}
#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct CryptoTradeUnitProto {
    #[prost(message, optional, tag = "1")]
    pub price: ::core::option::Option<MoneyProto>,
    #[prost(double, tag = "2")]
    pub lot_size: f64,
}
#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct StockTradeUnitProto {
    #[prost(string, tag = "1")]
    pub exchange: ::prost::alloc::string::String,
    #[prost(message, optional, tag = "2")]
    pub price: ::core::option::Option<MoneyProto>,
    #[prost(double, tag = "3")]
    pub lot_size: f64,
}
#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct CryptoQuotationProto {
    #[prost(string, tag = "1")]
    pub symbol: ::prost::alloc::string::String,
    #[prost(message, optional, tag = "2")]
    pub bid: ::core::option::Option<CryptoTradeUnitProto>,
    #[prost(message, optional, tag = "3")]
    pub ask: ::core::option::Option<CryptoTradeUnitProto>,
}
#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct CryptoTradeProto {
    #[prost(string, tag = "1")]
    pub symbol: ::prost::alloc::string::String,
    #[prost(int64, tag = "2")]
    pub trade_id: i64,
    #[prost(message, optional, tag = "3")]
    pub settle: ::core::option::Option<CryptoTradeUnitProto>,
    #[prost(string, tag = "4")]
    pub tks: ::prost::alloc::string::String,
}
#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct StockQuotationProto {
    #[prost(string, tag = "1")]
    pub symbol: ::prost::alloc::string::String,
    #[prost(message, optional, tag = "2")]
    pub bid: ::core::option::Option<StockTradeUnitProto>,
    #[prost(message, optional, tag = "3")]
    pub ask: ::core::option::Option<StockTradeUnitProto>,
    #[prost(string, repeated, tag = "4")]
    pub conditions: ::prost::alloc::vec::Vec<::prost::alloc::string::String>,
    #[prost(string, tag = "5")]
    pub tape: ::prost::alloc::string::String,
}
#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct StockTradeProto {
    #[prost(string, tag = "1")]
    pub symbol: ::prost::alloc::string::String,
    #[prost(int64, tag = "2")]
    pub trade_id: i64,
    #[prost(message, optional, tag = "3")]
    pub settle: ::core::option::Option<StockTradeUnitProto>,
    #[prost(string, repeated, tag = "4")]
    pub conditions: ::prost::alloc::vec::Vec<::prost::alloc::string::String>,
    #[prost(string, tag = "5")]
    pub tape: ::prost::alloc::string::String,
}
#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct MarketDataProto {
    #[prost(message, optional, tag = "1")]
    pub market_timestamp: ::core::option::Option<::prost_types::Timestamp>,
    #[prost(message, optional, tag = "2")]
    pub ingest_timestamp: ::core::option::Option<::prost_types::Timestamp>,
    #[prost(oneof = "market_data_proto::MessageType", tags = "3, 4, 5, 6")]
    pub message_type: ::core::option::Option<market_data_proto::MessageType>,
}
/// Nested message and enum types in `MarketDataProto`.
pub mod market_data_proto {
    #[allow(clippy::derive_partial_eq_without_eq)]
    #[derive(Clone, PartialEq, ::prost::Oneof)]
    pub enum MessageType {
        #[prost(message, tag = "3")]
        Cqm(super::CryptoQuotationProto),
        #[prost(message, tag = "4")]
        Ctm(super::CryptoTradeProto),
        #[prost(message, tag = "5")]
        Sqm(super::StockQuotationProto),
        #[prost(message, tag = "6")]
        Stm(super::StockTradeProto),
    }
}
