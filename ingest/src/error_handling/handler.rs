use std::fmt;

use crate::ProcessError;

pub fn handle_process_error(e: &ProcessError) {
    match e {
        ProcessError::AwsSdkError(msg) => eprintln!("AWS SDK error: {}", msg),
        ProcessError::EnvVarError(msg) => eprintln!("Environment variable error: {}", msg),
        /*
        ProcessError::ParsingError { msg, item_type } => {
            eprintln!("Error parsing {}: {}", item_type, msg)
        }
         */
        ProcessError::ProtobufConversionError(msg) => {
            eprintln!("Protobuf conversion error: {}", msg)
        }
        ProcessError::SerializationError(_) => eprintln!("Serialization error"),
        ProcessError::WebSocketConnectionError { url, source } => {
            eprintln!("Failed to connect to WebSocket server at {}: {}", url, source)
        }
        ProcessError::WebSocketCommunicationError(e) => {
            eprintln!("WebSocket communication error occurred: {}", e)
        }
        ProcessError::WebSocketReadError(err) => eprintln!("WebSocket read error: {}", err),
        ProcessError::UnknownItemType(item) => eprintln!("Unknown message type: {:?}", item),
        ProcessError::UrlParseError(msg) => eprintln!("Url parse error: {}", msg),
    }
}

impl fmt::Display for ProcessError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ProcessError::AwsSdkError(msg) => write!(f, "AWS SDK error: {}", msg),
            ProcessError::EnvVarError(msg) => write!(f, "Environment variable error: {}", msg),
            //ProcessError::ParsingError { msg, item_type } => write!(f, "Error parsing {}: {}", item_type, msg),
            ProcessError::ProtobufConversionError(msg) => {
                write!(f, "Protobuf conversion error: {}", msg)
            }
            ProcessError::SerializationError(_) => write!(f, "Serialization error"),
            ProcessError::WebSocketConnectionError { url, source } => {
                write!(f, "Failed to connect to WebSocket server at {}: {}", url, source)
            }
            ProcessError::WebSocketCommunicationError(e) => {
                write!(f, "WebSocket communication error occurred: {}", e)
            }
            ProcessError::WebSocketReadError(err) => write!(f, "WebSocket read error: {}", err),
            ProcessError::UnknownItemType(item) => write!(f, "Unknown message type: {:?}", item),
            ProcessError::UrlParseError(msg) => write!(f, "Url parse error: {}", msg),
        }
    }
}

impl std::error::Error for ProcessError {}
