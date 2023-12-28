use serde::Serialize;
use tokio_tungstenite::tungstenite::protocol::Message;

use crate::error_handling::ProcessError;
use crate::websocket::{process_message, read_from_connection, send_to_connection, AuthMessage, SubMessage};

pub async fn handle_websocket_stream() -> Result<(), ProcessError> {
    if let Err(e) = send_auth_message().await {
        return Err(e);
    }
    if let Err(e) = send_sub_message().await {
        return Err(e);
    }

    while let Ok(Some(message)) = read_from_connection().await {
        match process_message(message).await {
            Ok(_) => {}
            Err(e) => return Err(e),
        }
    }
    Ok(())
}

async fn send_message<T: Serialize>(message: &T) -> Result<(), ProcessError> {
    let ws_message = serde_json::to_string(message)?;
    send_to_connection(Message::Text(ws_message).to_string()).await?;
    Ok(())
}

async fn send_sub_message() -> Result<(), ProcessError> {
    let payload = SubMessage::new(vec!["MSFT".to_string()])?; // TODO: get tickers from config or use input
    send_message(&payload).await
}

async fn send_auth_message() -> Result<(), ProcessError> {
    let payload = AuthMessage::new()?;
    send_message(&payload).await
}
