use serde_json::Value;

#[derive(Debug)]
pub enum ProcessError {
    AwsSdkError(String),
    EnvVarError(String),
    /*
    ParsingError {
        msg: String,
        item_type: String,
    },
     */
    ProtobufConversionError(String),
    SerializationError(serde_json::Error),
    WebSocketConnectionError {
        url: String,
        source: tokio_tungstenite::tungstenite::Error,
    },
    WebSocketCommunicationError(tokio_tungstenite::tungstenite::Error),
    WebSocketReadError(tokio_tungstenite::tungstenite::Error),
    UnknownItemType(Value),
    UrlParseError(url::ParseError),
}
