use config::{Config, ConfigError, Environment};
use serde::Deserialize;

#[derive(Clone, Debug, Deserialize)]
pub enum FeedType {
    Stocks,
    Crypto,
}

#[derive(Clone, Debug, Deserialize)]
pub struct WebSocketFeed {
    pub url: String,
    pub symbols: Vec<String>,
    pub feed_type: FeedType,
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
