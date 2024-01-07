#[derive(Debug)]
pub enum ProcessError {
    AwsSdkError(String),
    ConfigError(String),
    ConnectionNotFound(String),
    EnvVarError(String),
    JsonDeOrSerializationError(serde_json::Error),
    KinesisSendError(String),
    MaxRetriesReached(String),
    ProtobufConversionError(String),
    //ReconnectionFailed(String),
    //TaskPanicked(String),
    UrlParseError(url::ParseError),
    WebSocketConnectionError {
        url: String,
        source: tokio_tungstenite::tungstenite::Error,
    },
    WebSocketCommunicationError(tokio_tungstenite::tungstenite::Error),
    //WebSocketReadError(tokio_tungstenite::tungstenite::Error),
}
