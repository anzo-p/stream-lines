#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct CryptoQuotationMessageProto {
    #[prost(string, tag = "1")]
    pub ty: ::prost::alloc::string::String,
    #[prost(string, tag = "2")]
    pub sy: ::prost::alloc::string::String,
    #[prost(int64, tag = "3")]
    pub bpu: i64,
    #[prost(int64, tag = "4")]
    pub bpf: i64,
    #[prost(double, tag = "5")]
    pub bs: f64,
    #[prost(int64, tag = "6")]
    pub apu: i64,
    #[prost(int64, tag = "7")]
    pub apf: i64,
    #[prost(double, tag = "8")]
    pub r#as: f64,
    #[prost(message, optional, tag = "9")]
    pub ti: ::core::option::Option<::prost_types::Timestamp>,
}
