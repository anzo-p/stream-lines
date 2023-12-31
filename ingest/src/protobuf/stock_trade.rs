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
    pub p: ::core::option::Option<super::money::MoneyProto>,
    #[prost(double, tag = "6")]
    pub s: f64,
    #[prost(string, repeated, tag = "7")]
    pub c: ::prost::alloc::vec::Vec<::prost::alloc::string::String>,
    #[prost(message, optional, tag = "8")]
    pub ti: ::core::option::Option<::prost_types::Timestamp>,
    #[prost(string, tag = "9")]
    pub z: ::prost::alloc::string::String,
}
