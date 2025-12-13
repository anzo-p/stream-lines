use std::process;

use crate::ProcessError;

pub fn handle_process_error(e: &ProcessError) {
    match e {
        ProcessError::AwsSdkError(msg) => {
            eprintln!("AWS SDK error: {}", msg);
            process::exit(1)
        }
        ProcessError::ConfigError(msg) => {
            eprintln!("Config error: {}", msg);
            process::exit(1)
        }
        ProcessError::ConnectionNotFound(msg) => {
            eprintln!("Connection not found: {}", msg)
        }
        ProcessError::EnvVarError(msg) => {
            eprintln!("Environment variable error: {}", msg);
            process::exit(1)
        }
        ProcessError::JsonDeOrSerializationError(msg) => {
            eprintln!("JSON De- or Serialization error {}", msg)
        }
        ProcessError::KinesisSendError(msg) => {
            eprintln!("Kinesis send error: {}", msg)
        }
        ProcessError::MaxRetriesReached(msg) => {
            eprintln!("Max retries reached: {}", msg)
        }
        ProcessError::ProtobufConversionError(msg) => {
            eprintln!("Protobuf conversion error: {}", msg)
        }
        ProcessError::UrlParseError(msg) => {
            eprintln!("Url parse error: {}", msg)
        }
        ProcessError::WebSocketConnectionError { url, source } => {
            eprintln!("Failed to connect to WebSocket server at {}: {}", url, source)
        }
        ProcessError::WebSocketCommunicationError(e) => {
            eprintln!("WebSocket communication error occurred: {}", e)
        }
        ProcessError::WebSocketFeedError(msg) => {
            eprintln!("WebSocket auth error: {}", msg);
        }
    }
}
