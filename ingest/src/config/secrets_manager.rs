use crate::errors::ProcessError;
use aws_config::BehaviorVersion;
use aws_sdk_secretsmanager as sm;
use serde::Deserialize;
use tokio::sync::OnceCell;

static SHARED_SECRET: OnceCell<String> = OnceCell::const_new();

#[derive(Debug, Deserialize)]
struct SharedSecretJson {
    #[serde(rename = "internal.shared-secret")]
    value: String,
}

pub async fn get_internal_shared_secret() -> Result<&'static str, ProcessError> {
    SHARED_SECRET
        .get_or_try_init(|| async {
            let secret_id = "prod/internal/shared-secret";
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

            let parsed: SharedSecretJson = serde_json::from_str(s)
                .map_err(|e| ProcessError::AwsSdkError(format!("secret JSON parse failed: {e}")))?;

            Ok::<String, ProcessError>(parsed.value)
        })
        .await
        .map(|s| s.as_str())
}
