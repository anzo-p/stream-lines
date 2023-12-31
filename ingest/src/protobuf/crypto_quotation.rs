#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct CryptoQuotationMessageProto {
    #[prost(string, tag = "1")]
    pub ty: ::prost::alloc::string::String,
    #[prost(string, tag = "2")]
    pub sy: ::prost::alloc::string::String,
    #[prost(message, optional, tag = "3")]
    pub bp: ::core::option::Option<super::money::MoneyProto>,
    #[prost(double, tag = "4")]
    pub bs: f64,
    #[prost(message, optional, tag = "5")]
    pub ap: ::core::option::Option<super::money::MoneyProto>,
    #[prost(double, tag = "6")]
    pub r#as: f64,
    #[prost(message, optional, tag = "7")]
    pub ti: ::core::option::Option<::prost_types::Timestamp>,
}
