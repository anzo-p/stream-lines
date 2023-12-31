#[derive(Debug)]
pub enum ProcessError {
    AwsSdkError(String),
    ConfigError(String),
    EnvVarError(String),
    KinesisSendError(String),
    ProtobufConversionError(String),
    SerializationError(serde_json::Error),
    WebSocketConnectionError {
        url: String,
        source: tokio_tungstenite::tungstenite::Error,
    },
    WebSocketCommunicationError(tokio_tungstenite::tungstenite::Error),
    WebSocketReadError(tokio_tungstenite::tungstenite::Error),
    UrlParseError(url::ParseError),
}
