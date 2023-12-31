#[allow(clippy::derive_partial_eq_without_eq)]
#[derive(Clone, PartialEq, ::prost::Message)]
pub struct MoneyProto {
    #[prost(int64, tag = "1")]
    pub u: i64,
    #[prost(double, tag = "2")]
    pub n: f64,
    #[prost(string, tag = "3")]
    pub c: ::prost::alloc::string::String,
}
