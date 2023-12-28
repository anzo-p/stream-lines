use futures_util::{stream::StreamExt, SinkExt};
use serde::Serialize;
use std::env;
use tokio::net::TcpStream;
use tokio_tungstenite::{
    connect_async, tungstenite::protocol::Message, MaybeTlsStream, WebSocketStream,
};
use url::Url;

use crate::error_handling::ProcessError;
use crate::websocket::{process_message, AuthMessage, SubMessage};

pub async fn connect_to_feed() -> Result<WebSocketStream<MaybeTlsStream<TcpStream>>, ProcessError> {
    let url_str = env::var("MARKET_DATA_URL")?;
    let url = Url::parse(&*url_str)?;
    let (ws_stream, _) =
        connect_async(&url)
            .await
            .map_err(|e| ProcessError::WebSocketConnectionError {
                url: url.to_string(),
                source: e,
            })?;

    Ok(ws_stream)
}

pub async fn handle_websocket_stream(
    mut ws_stream: WebSocketStream<MaybeTlsStream<TcpStream>>,
) -> Result<(), ProcessError> {
    if let Err(e) = send_auth_message(&mut ws_stream).await {
        return Err(e);
    }
    if let Err(e) = send_sub_message(&mut ws_stream).await {
        return Err(e);
    }

    let (_, mut read) = ws_stream.split();

    while let Some(message) = read.next().await {
        match message {
            Ok(msg) => {
                if let Err(e) = process_message(msg).await {
                    return Err(e);
                }
            }
            Err(e) => return Err(ProcessError::WebSocketReadError(e)),
        }
    }
    Ok(())
}

async fn send_message<T: Serialize>(
    ws_stream: &mut WebSocketStream<MaybeTlsStream<TcpStream>>,
    message: &T,
) -> Result<(), ProcessError> {
    let ws_message = serde_json::to_string(message)?;
    ws_stream.send(Message::Text(ws_message)).await?;
    Ok(())
}

async fn send_sub_message(
    ws_stream: &mut WebSocketStream<MaybeTlsStream<TcpStream>>,
) -> Result<(), ProcessError> {
    let payload = SubMessage::new(vec!["MSFT".to_string()])?; // TODO: get tickers from config or use input
    send_message(ws_stream, &payload).await
}

async fn send_auth_message(
    ws_stream: &mut WebSocketStream<MaybeTlsStream<TcpStream>>,
) -> Result<(), ProcessError> {
    let payload = AuthMessage::new()?;
    send_message(ws_stream, &payload).await
}
