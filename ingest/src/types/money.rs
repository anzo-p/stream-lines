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
    let v = serde_json::Value::deserialize(deserializer)?;
    if let Some(f) = v.as_f64() {
        return MoneyMessageVisitor.visit_f64(f);
    }
    if let Some(i) = v.as_i64() {
        return MoneyMessageVisitor.visit_i64(i);
    }
    Err(de::Error::custom("Invalid type for MoneyMessage"))
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

    fn visit_i64<E>(self, value: i64) -> Result<MoneyMessage, E>
    where
        E: de::Error,
    {
        Ok(MoneyMessage {
            units: value,
            nanos: 0.0,
            currency: "USD".to_string(),
        })
    }
}
