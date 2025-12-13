use crate::config::secrets_manager::get_internal_shared_secret;
use crate::config::{AppConfig, FeedType};
use crate::errors::ProcessError;
use log::warn;
use reqwest::Client;
use serde::Deserialize;
use std::collections::HashMap;
use std::env;
use tokio::time::{timeout, Duration};

#[derive(Debug, Deserialize)]
pub struct TopTradedTickers(pub HashMap<String, HashMap<String, f64>>);

async fn fetch_top_symbols(client: &Client, endpoint: &str, api_key: &str, n: usize) -> anyhow::Result<Vec<String>> {
    let data = client
        .get(endpoint)
        .header("X-Api-Key", api_key)
        .send()
        .await?
        .error_for_status()?
        .json::<TopTradedTickers>()
        .await?;

    let (_, tickers) = data.0.into_iter().next().unwrap();
    let mut items: Vec<(String, f64)> = tickers.into_iter().collect();
    items.sort_by(|a, b| b.1.total_cmp(&a.1));

    Ok(items.into_iter().take(n).map(|(sym, _)| sym).collect())
}

pub(crate) async fn hydrate_symbols(mut cfg: AppConfig) -> Result<AppConfig, ProcessError> {
    let endpoint =
        env::var("TOP_TICKERS_API").map_err(|_| ProcessError::ConfigError("TOP_TICKERS_API not set".into()))?;

    let max_n: usize = env::var("MAX_TICKER_COUNT")
        .ok()
        .and_then(|s| s.parse().ok())
        .unwrap_or(30);

    let client = Client::new();

    if let Some(feed) = cfg.feeds.iter_mut().find(|f| matches!(f.feed_type, FeedType::Stocks)) {
        match timeout(Duration::from_secs(15), async {
            let api_key = get_internal_shared_secret().await?;
            fetch_top_symbols(&client, &endpoint, api_key, max_n).await
        })
        .await
        {
            Ok(Ok(syms)) => feed.symbols = syms,
            Ok(Err(e)) => warn!("Ticker fetch failed: {e}. Using config defaults."),
            Err(_) => warn!("Ticker fetch timed out. Using config defaults."),
        }
    } else {
        warn!("No Stocks feed configured. Leaving feeds unchanged.");
    }

    Ok(cfg)
}
