use serde::{Deserialize, Serialize};
use std::env;

use crate::error_handling::ProcessError;

#[derive(Serialize, Deserialize)]
pub struct AuthMessage {
    action: String,
    key: String,
    secret: String,
}

impl AuthMessage {
    pub fn new() -> Result<Self, ProcessError> {
        let key = env::var("ALPACA_API_KEY")
            .map_err(|_| ProcessError::EnvVarError(String::from("ALPACA_API_KEY not found in environment")))?;
        let secret = env::var("ALPACA_API_SECRET")
            .map_err(|_| ProcessError::EnvVarError(String::from("ALPACA_API_SECRET not found in environment")))?;

        Ok(AuthMessage {
            action: "auth".to_string(),
            key,
            secret,
        })
    }
}

#[derive(Serialize)]
pub struct SubMessage {
    action: String,
    trades: Vec<String>,
    quotes: Vec<String>,
}

impl SubMessage {
    pub fn new(trades: &Vec<String>) -> Result<Self, ProcessError> {
        Ok(SubMessage {
            action: "subscribe".to_string(),
            trades: trades.clone(),
            quotes: trades.clone(),
        })
    }
}
