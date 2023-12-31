#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct CryptoTradeMessageProto {
    #[prost(string, tag = "1")]
    pub ty: ::prost::alloc::string::String,
    #[prost(string, tag = "2")]
    pub sy: ::prost::alloc::string::String,
    #[prost(int64, tag = "3")]
    pub i: i64,
    #[prost(int64, tag = "4")]
    pub pu: i64,
    #[prost(int64, tag = "5")]
    pub pf: i64,
    #[prost(double, tag = "6")]
    pub s: f64,
    #[prost(message, optional, tag = "7")]
    pub ti: ::core::option::Option<::prost_types::Timestamp>,
    #[prost(string, tag = "8")]
    pub tks: ::prost::alloc::string::String,
}
