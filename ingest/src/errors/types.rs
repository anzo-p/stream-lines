use std::fmt;

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
    UrlParseError(url::ParseError),
    WebSocketConnectionError {
        url: String,
        source: tokio_tungstenite::tungstenite::Error,
    },
    WebSocketCommunicationError(tokio_tungstenite::tungstenite::Error),
}

impl fmt::Display for ProcessError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ProcessError::AwsSdkError(msg) => write!(f, "AWS SDK error: {}", msg),
            ProcessError::ConfigError(msg) => write!(f, "Config error: {}", msg),
            ProcessError::ConnectionNotFound(msg) => write!(f, "Connection not found: {}", msg),
            ProcessError::EnvVarError(msg) => write!(f, "Environment variable error: {}", msg),
            ProcessError::KinesisSendError(msg) => write!(f, "Kinesis send error: {}", msg),
            ProcessError::MaxRetriesReached(msg) => write!(f, "Max retries reached: {}", msg),
            ProcessError::ProtobufConversionError(msg) => {
                write!(f, "Protobuf conversion error: {}", msg)
            }
            ProcessError::JsonDeOrSerializationError(msg) => write!(f, "JSON De- or Serialization error {}", msg),
            ProcessError::UrlParseError(msg) => write!(f, "Url parse error: {}", msg),
            ProcessError::WebSocketConnectionError { url, source } => {
                write!(f, "Failed to connect to WebSocket server at {}: {}", url, source)
            }
            ProcessError::WebSocketCommunicationError(e) => {
                write!(f, "WebSocket communication error occurred: {}", e)
            }
        }
    }
}

//impl std::error::Error for ProcessError {}
