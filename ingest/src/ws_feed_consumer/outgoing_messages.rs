use crate::errors::ProcessError;
use aws_config::BehaviorVersion;
use aws_sdk_secretsmanager as sm;
use serde::{Deserialize, Serialize};
use tokio::sync::OnceCell;

static CREDS: OnceCell<AlpacaCreds> = OnceCell::const_new();

#[derive(Debug, Clone, Deserialize)]
struct AlpacaCreds {
    #[serde(rename = "alpaca.authentication.api-key")]
    key: String,
    #[serde(rename = "alpaca.authentication.api-secret")]
    secret: String,
}

async fn get_creds() -> Result<&'static AlpacaCreds, ProcessError> {
    CREDS
        .get_or_try_init(|| async { load_alpaca_creds("prod/alpaca/api").await })
        .await
}

#[derive(Serialize, Deserialize)]
pub struct AuthMessage {
    action: String,
    key: String,
    secret: String,
}

impl AuthMessage {
    pub async fn new() -> Result<Self, ProcessError> {
        let creds = get_creds().await?;
        Ok(Self {
            action: "auth".to_string(),
            key: creds.key.clone(),
            secret: creds.secret.clone(),
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

async fn load_alpaca_creds(secret_id: &str) -> Result<AlpacaCreds, ProcessError> {
    let config = aws_config::load_defaults(BehaviorVersion::latest()).await;
    let client = sm::Client::new(&config);

    let out = client
        .get_secret_value()
        .secret_id(secret_id)
        .send()
        .await
        .map_err(|e| ProcessError::AwsSdkError(format!("get_secret_value failed for {secret_id}: {e:?}")))?;

    let s = out
        .secret_string()
        .ok_or_else(|| ProcessError::AwsSdkError(format!("secret {secret_id} was binary or empty")))?;

    serde_json::from_str::<AlpacaCreds>(s)
        .map_err(|e| ProcessError::AwsSdkError(format!("secret JSON parse failed for {secret_id}: {e}")))
}
