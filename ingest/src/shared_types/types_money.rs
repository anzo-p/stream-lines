use serde::de::{self, Visitor};
use serde::{Deserialize, Deserializer, Serialize};
use std::fmt;

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct MoneyMessage {
    pub units: i64,
    pub nanos: f64,
    pub currency: String,
}

struct MoneyMessageVisitor;

pub fn deserialize_money_message<'de, D>(deserializer: D) -> Result<MoneyMessage, D::Error>
where
    D: Deserializer<'de>,
{
    deserializer.deserialize_any(MoneyMessageVisitor)
}

impl<'de> Visitor<'de> for MoneyMessageVisitor {
    type Value = MoneyMessage;

    fn expecting(&self, formatter: &mut fmt::Formatter) -> fmt::Result {
        formatter.write_str("a floating point number for MoneyMessage")
    }

    fn visit_f64<E>(self, value: f64) -> Result<MoneyMessage, E>
    where
        E: de::Error,
    {
        let units = value.trunc() as i64;
        let nanos = (value.fract() * 1_000_000_000.0).round();

        Ok(MoneyMessage {
            units,
            nanos,
            currency: "USD".to_string(),
        })
    }
}
