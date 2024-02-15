use serde::de::{self, Visitor};
use serde::{Deserialize, Deserializer, Serialize};
use std::fmt;

#[derive(Serialize, Deserialize, Clone, Debug)]
pub struct MoneyMessage {
    pub units: i64,
    pub nanos: i32,
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
        let nanos = (value.fract() * 1_000_000_000.0).round() as i32;

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
            nanos: 0,
            currency: "USD".to_string(),
        })
    }
}

#[cfg(test)]
mod tests {
    use super::*;
    use serde_json::json;

    #[test]
    fn test_deserialize_money_message_integer_string_form() {
        let json_value = json!(123);

        let money_message: MoneyMessage = deserialize_money_message(&json_value).unwrap();

        assert_eq!(money_message.units, 123);
        assert_eq!(money_message.nanos, 0);
        assert_eq!(money_message.currency, "USD");
    }

    #[test]
    fn test_deserialize_money_message_floating_string_form() {
        let json_value = json!(123.45);

        let money_message: MoneyMessage = deserialize_money_message(&json_value).unwrap();

        assert_eq!(money_message.units, 123);
        assert_eq!(money_message.nanos, 45 * 10 * 1_000_000);
        assert_eq!(money_message.currency, "USD");
    }

    #[test]
    fn test_deserialize_money_message_integer_string_negative_form() {
        let json_value = json!(-123);

        let money_message: MoneyMessage = deserialize_money_message(&json_value).unwrap();

        assert_eq!(money_message.units, -123);
        assert_eq!(money_message.nanos, 0);
        assert_eq!(money_message.currency, "USD");
    }

    #[test]
    fn test_deserialize_money_message_floating_string_negative_form() {
        let json_value = json!(-123.45);

        let money_message: MoneyMessage = deserialize_money_message(&json_value).unwrap();

        assert_eq!(money_message.units, -123);
        assert_eq!(money_message.nanos, -45 * 10 * 1_000_000);
        assert_eq!(money_message.currency, "USD");
    }

    #[test]
    fn test_deserialize_money_message_floating_string_precision_limit() {
        let json_value = json!(0.000000001);

        let money_message: MoneyMessage = deserialize_money_message(&json_value).unwrap();

        assert_eq!(money_message.units, 0);
        assert_eq!(money_message.nanos, 1);
        assert_eq!(money_message.currency, "USD");
    }

    #[test]
    fn test_deserialize_money_message_nano_rounds_up() {
        let json_value = json!(0.0000000005);

        let money_message: MoneyMessage = deserialize_money_message(&json_value).unwrap();

        assert_eq!(money_message.units, 0);
        assert_eq!(money_message.nanos, 1);
        assert_eq!(money_message.currency, "USD");
    }

    #[test]
    fn test_deserialize_money_message_nano_rounds_down() {
        let json_value = json!(0.0000000004);

        let money_message: MoneyMessage = deserialize_money_message(&json_value).unwrap();

        assert_eq!(money_message.units, 0);
        assert_eq!(money_message.nanos, 0);
        assert_eq!(money_message.currency, "USD");
    }
}
