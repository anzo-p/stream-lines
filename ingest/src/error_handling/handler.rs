use crate::ProcessError;

pub fn handle_process_error(e: &ProcessError) {
    match e {
        ProcessError::EnvVarError(msg) => eprintln!("Environment variable error: {}", msg),
        ProcessError::ParsingError { msg, item_type } => {
            eprintln!("Error parsing {}: {}", item_type, msg)
        }
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
