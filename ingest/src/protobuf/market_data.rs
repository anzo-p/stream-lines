#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct MoneyMessageProto {
    #[prost(int64, tag = "1")]
    pub u: i64,
    #[prost(double, tag = "2")]
    pub n: f64,
    #[prost(string, tag = "3")]
    pub c: ::prost::alloc::string::String,
}
#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct CryptoQuotationMessageProto {
    #[prost(string, tag = "1")]
    pub ty: ::prost::alloc::string::String,
    #[prost(string, tag = "2")]
    pub sy: ::prost::alloc::string::String,
    #[prost(message, optional, tag = "3")]
    pub bp: ::core::option::Option<MoneyMessageProto>,
    #[prost(double, tag = "4")]
    pub bs: f64,
    #[prost(message, optional, tag = "5")]
    pub ap: ::core::option::Option<MoneyMessageProto>,
    #[prost(double, tag = "6")]
    pub r#as: f64,
    #[prost(message, optional, tag = "7")]
    pub ti: ::core::option::Option<::prost_types::Timestamp>,
}
#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct CryptoTradeMessageProto {
    #[prost(string, tag = "1")]
    pub ty: ::prost::alloc::string::String,
    #[prost(string, tag = "2")]
    pub sy: ::prost::alloc::string::String,
    #[prost(int64, tag = "3")]
    pub i: i64,
    #[prost(message, optional, tag = "4")]
    pub p: ::core::option::Option<MoneyMessageProto>,
    #[prost(double, tag = "5")]
    pub s: f64,
    #[prost(message, optional, tag = "6")]
    pub ti: ::core::option::Option<::prost_types::Timestamp>,
    #[prost(string, tag = "7")]
    pub tks: ::prost::alloc::string::String,
}
#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct StockQuotationMessageProto {
    #[prost(string, tag = "1")]
    pub ty: ::prost::alloc::string::String,
    #[prost(string, tag = "2")]
    pub sy: ::prost::alloc::string::String,
    #[prost(string, tag = "3")]
    pub bx: ::prost::alloc::string::String,
    #[prost(message, optional, tag = "4")]
    pub bp: ::core::option::Option<MoneyMessageProto>,
    #[prost(double, tag = "5")]
    pub bs: f64,
    #[prost(string, tag = "6")]
    pub ax: ::prost::alloc::string::String,
    #[prost(message, optional, tag = "7")]
    pub ap: ::core::option::Option<MoneyMessageProto>,
    #[prost(double, tag = "8")]
    pub r#as: f64,
    #[prost(message, optional, tag = "9")]
    pub ti: ::core::option::Option<::prost_types::Timestamp>,
    #[prost(string, repeated, tag = "10")]
    pub c: ::prost::alloc::vec::Vec<::prost::alloc::string::String>,
    #[prost(string, tag = "11")]
    pub z: ::prost::alloc::string::String,
}
#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct StockTradeMessageProto {
    #[prost(string, tag = "1")]
    pub ty: ::prost::alloc::string::String,
    #[prost(string, tag = "2")]
    pub sy: ::prost::alloc::string::String,
    #[prost(int64, tag = "3")]
    pub i: i64,
    #[prost(string, tag = "4")]
    pub x: ::prost::alloc::string::String,
    #[prost(message, optional, tag = "5")]
    pub p: ::core::option::Option<MoneyMessageProto>,
    #[prost(double, tag = "6")]
    pub s: f64,
    #[prost(string, repeated, tag = "7")]
    pub c: ::prost::alloc::vec::Vec<::prost::alloc::string::String>,
    #[prost(message, optional, tag = "8")]
    pub ti: ::core::option::Option<::prost_types::Timestamp>,
    #[prost(string, tag = "9")]
    pub z: ::prost::alloc::string::String,
}
#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct MarketDataMessageProto {
    #[prost(oneof = "market_data_message_proto::MessageType", tags = "1, 2, 3, 4")]
    pub message_type: ::core::option::Option<market_data_message_proto::MessageType>,
}
/// Nested message and enum types in `MarketDataMessageProto`.
pub mod market_data_message_proto {
    #[allow(clippy::derive_partial_eq_without_eq)]
    #[derive(Clone, PartialEq, ::prost::Oneof)]
    pub enum MessageType {
        #[prost(message, tag = "1")]
        Cqm(super::CryptoQuotationMessageProto),
        #[prost(message, tag = "2")]
        Ctm(super::CryptoTradeMessageProto),
        #[prost(message, tag = "3")]
        Sqm(super::StockQuotationMessageProto),
        #[prost(message, tag = "4")]
        Stm(super::StockTradeMessageProto),
    }
}
