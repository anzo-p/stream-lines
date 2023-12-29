#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct QuotationMessageProto {
    #[prost(string, tag = "1")]
    pub ty: ::prost::alloc::string::String,
    #[prost(string, tag = "2")]
    pub sy: ::prost::alloc::string::String,
    #[prost(string, tag = "3")]
    pub bx: ::prost::alloc::string::String,
    #[prost(int32, tag = "4")]
    pub bpu: i32,
    #[prost(int32, tag = "5")]
    pub bpf: i32,
    #[prost(double, tag = "6")]
    pub bs: f64,
    #[prost(string, tag = "7")]
    pub ax: ::prost::alloc::string::String,
    #[prost(int32, tag = "8")]
    pub apu: i32,
    #[prost(int32, tag = "9")]
    pub apf: i32,
    #[prost(double, tag = "10")]
    pub r#as: f64,
    #[prost(message, optional, tag = "11")]
    pub ti: ::core::option::Option<::prost_types::Timestamp>,
    #[prost(string, repeated, tag = "12")]
    pub c: ::prost::alloc::vec::Vec<::prost::alloc::string::String>,
    #[prost(string, tag = "13")]
    pub z: ::prost::alloc::string::String,
}
