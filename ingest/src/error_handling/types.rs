#[derive(Debug)]
pub enum ProcessError {
    AwsSdkError(String),
    ConfigError(String),
    EnvVarError(String),
    JsonDeOrSerializationError(serde_json::Error),
    KinesisSendError(String),
    ProtobufConversionError(String),
    WebSocketConnectionError {
        url: String,
        source: tokio_tungstenite::tungstenite::Error,
    },
    WebSocketCommunicationError(tokio_tungstenite::tungstenite::Error),
    WebSocketReadError(tokio_tungstenite::tungstenite::Error),
    UrlParseError(url::ParseError),
}
