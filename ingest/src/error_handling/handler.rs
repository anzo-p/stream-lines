use std::fmt;

use crate::ProcessError;

pub fn handle_process_error(e: &ProcessError) {
    match e {
        ProcessError::AwsSdkError(msg) => eprintln!("AWS SDK error: {}", msg),
        ProcessError::ConfigError(msg) => eprintln!("Config error: {}", msg),
        ProcessError::EnvVarError(msg) => eprintln!("Environment variable error: {}", msg),
        ProcessError::KinesisSendError(msg) => eprintln!("Kinesis send error: {}", msg),
        ProcessError::ProtobufConversionError(msg) => {
            eprintln!("Protobuf conversion error: {}", msg)
        }
        ProcessError::JsonDeOrSerializationError(msg) => eprintln!("JSON De- or Serialization error {}", msg),
        ProcessError::WebSocketConnectionError { url, source } => {
            eprintln!("Failed to connect to WebSocket server at {}: {}", url, source)
        }
        ProcessError::WebSocketCommunicationError(e) => {
            eprintln!("WebSocket communication error occurred: {}", e)
        }
        //ProcessError::WebSocketReadError(err) => eprintln!("WebSocket read error: {}", err),
        ProcessError::UrlParseError(msg) => eprintln!("Url parse error: {}", msg),
    }
}

impl fmt::Display for ProcessError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ProcessError::AwsSdkError(msg) => write!(f, "AWS SDK error: {}", msg),
            ProcessError::ConfigError(msg) => write!(f, "Config error: {}", msg),
            ProcessError::EnvVarError(msg) => write!(f, "Environment variable error: {}", msg),
            ProcessError::KinesisSendError(msg) => write!(f, "Kinesis send error: {}", msg),
            ProcessError::ProtobufConversionError(msg) => {
                write!(f, "Protobuf conversion error: {}", msg)
            }
            ProcessError::JsonDeOrSerializationError(msg) => write!(f, "JSON De- or Serialization error {}", msg),
            ProcessError::WebSocketConnectionError { url, source } => {
                write!(f, "Failed to connect to WebSocket server at {}: {}", url, source)
            }
            ProcessError::WebSocketCommunicationError(e) => {
                write!(f, "WebSocket communication error occurred: {}", e)
            }
            //ProcessError::WebSocketReadError(err) => write!(f, "WebSocket read error: {}", err),
            ProcessError::UrlParseError(msg) => write!(f, "Url parse error: {}", msg),
        }
    }
}

impl std::error::Error for ProcessError {}
