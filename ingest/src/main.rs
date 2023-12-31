mod app_config;
mod error_handling;
mod protobuf;
mod shared_types;
mod stream_producer;
mod websocket;

use dotenv;

use crate::app_config::AppConfig;
use crate::error_handling::ProcessError;
use crate::websocket::run_feeds;

fn load_app_config() -> Result<AppConfig, ProcessError> {
    AppConfig::new().map_err(|e| ProcessError::ConfigError(e.to_string()))
}

#[tokio::main]
async fn main() -> Result<(), ProcessError> {
    dotenv::dotenv().ok();

    let app_config = load_app_config()?;
    run_feeds(app_config).await
}
