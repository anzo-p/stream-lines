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
    pub bp: ::core::option::Option<super::money::MoneyProto>,
    #[prost(double, tag = "5")]
    pub bs: f64,
    #[prost(string, tag = "6")]
    pub ax: ::prost::alloc::string::String,
    #[prost(message, optional, tag = "7")]
    pub ap: ::core::option::Option<super::money::MoneyProto>,
    #[prost(double, tag = "8")]
    pub r#as: f64,
    #[prost(message, optional, tag = "9")]
    pub ti: ::core::option::Option<::prost_types::Timestamp>,
    #[prost(string, repeated, tag = "10")]
    pub c: ::prost::alloc::vec::Vec<::prost::alloc::string::String>,
    #[prost(string, tag = "11")]
    pub z: ::prost::alloc::string::String,
}
