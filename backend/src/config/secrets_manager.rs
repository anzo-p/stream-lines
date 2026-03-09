use crate::errors::ProcessError;
use aws_config::BehaviorVersion;
use aws_sdk_secretsmanager as sm;
use serde::Deserialize;
use tokio::sync::OnceCell;

static MARKET_DATA_HISTORICAL_READ_TOKEN: OnceCell<String> = OnceCell::const_new();

#[derive(Debug, Deserialize)]
struct MarketDataHistoricalReadJson {
    #[serde(rename = "influxdb.token")]
    value: String,
}

pub async fn get_influxdb_token() -> Result<&'static str, ProcessError> {
    MARKET_DATA_HISTORICAL_READ_TOKEN
        .get_or_try_init(|| async {
            let secret_id = "prod/influxdb/market-data-historical/read";
            let config = aws_config::load_defaults(BehaviorVersion::latest()).await;
            let client = sm::Client::new(&config);

            let out = client
                .get_secret_value()
                .secret_id(secret_id)
                .send()
                .await
                .map_err(|e| ProcessError::AwsSdkError(format!("get_secret_value failed: {e}")))?;

            let s = out
                .secret_string()
                .ok_or_else(|| ProcessError::AwsSdkError("secret was binary or empty".into()))?;

            let parsed: MarketDataHistoricalReadJson = serde_json::from_str(s)
                .map_err(|e| ProcessError::AwsSdkError(format!("secret JSON parse failed: {e}")))?;

            Ok::<String, ProcessError>(parsed.value)
        })
        .await
        .map(|s| s.as_str())
}
