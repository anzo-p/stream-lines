use serde_json::Value;
use std::env;

#[derive(Debug)]
pub enum ProcessError {
    EnvVarError(String),
    ParsingError {
        msg: String,
        item_type: String,
    },
    #[allow(dead_code)]
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

impl From<serde_json::Error> for ProcessError {
    fn from(err: serde_json::Error) -> Self {
        ProcessError::SerializationError(err)
    }
}

impl From<tokio_tungstenite::tungstenite::Error> for ProcessError {
    fn from(err: tokio_tungstenite::tungstenite::Error) -> Self {
        ProcessError::WebSocketCommunicationError(err)
    }
}

impl From<env::VarError> for ProcessError {
    fn from(err: env::VarError) -> Self {
        ProcessError::EnvVarError(err.to_string())
    }
}

impl From<url::ParseError> for ProcessError {
    fn from(err: url::ParseError) -> Self {
        ProcessError::UrlParseError(err)
    }
}
