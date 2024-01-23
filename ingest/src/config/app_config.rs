use config::{Config, ConfigError, Environment};
use serde::Deserialize;
use std::fmt;

#[derive(Clone, Debug, Deserialize)]
pub enum FeedType {
    Stocks,
    Crypto,
}

impl fmt::Display for FeedType {
    fn fmt(&self, f: &mut fmt::Formatter) -> fmt::Result {
        match self {
            FeedType::Stocks => write!(f, "Stocks"),
            FeedType::Crypto => write!(f, "Crypto"),
        }
    }
}

#[derive(Clone, Debug, Deserialize)]
pub struct WebSocketFeed {
    pub url: String,
    pub symbols: Vec<String>,
    pub feed_type: FeedType,
    pub max_reads_per_sec: u64,
}

#[derive(Debug, Deserialize)]
pub struct AppConfig {
    pub feeds: Vec<WebSocketFeed>,
}

impl AppConfig {
    pub fn new() -> Result<Self, ConfigError> {
        Config::builder()
            .add_source(config::File::with_name("app_config.json"))
            .add_source(Environment::default().separator("__"))
            .build()?
            .try_deserialize()
    }
}
