use futures_util::{stream::StreamExt, SinkExt};
use lazy_static::lazy_static;
use std::{env, sync::Mutex};
use tokio::net::TcpStream;
use tokio_tungstenite::{connect_async, tungstenite::Message, MaybeTlsStream, WebSocketStream};
use url::Url;

use crate::error_handling::ProcessError;

lazy_static! {
    static ref WEBSOCKET_CONNECTION: Mutex<Option<WebSocketStream<MaybeTlsStream<TcpStream>>>> = Mutex::new(None);
}

pub async fn read_from_connection() -> Result<Option<Message>, ProcessError> {
    let mut ws_connection = WEBSOCKET_CONNECTION.lock().unwrap();
    if let Some(ws) = ws_connection.as_mut() {
        let (_, mut read) = ws.split();

        match read.next().await {
            Some(Ok(message)) => {
                return Ok(Some(message));
            }
            Some(Err(e)) => return Err(ProcessError::WebSocketReadError(e)),
            None => return Ok(None),
        }
    }
    Ok(None)
}

pub async fn send_to_connection(message: String) -> Result<(), ProcessError> {
    let mut ws_connection = WEBSOCKET_CONNECTION.lock().unwrap();
    if let Some(ws) = ws_connection.as_mut() {
        ws.send(Message::from(message)).await?;
    }
    Ok(())
}

pub async fn connect_to_stream() -> Result<(), ProcessError> {
    let mut ws_connection = WEBSOCKET_CONNECTION.lock().unwrap();

    if ws_connection.is_none() {
        let url_str = env::var("MARKET_DATA_URL")
            .map_err(|_| ProcessError::EnvVarError(String::from("MARKET_DATA_URL not found in environment")))?;

        let url = Url::parse(&url_str).map_err(|e| ProcessError::UrlParseError(e))?;

        let (ws_stream, _) = connect_async(&url)
            .await
            .map_err(|e| ProcessError::WebSocketConnectionError {
                url: url.to_string(),
                source: e,
            })?;

        *ws_connection = Some(ws_stream);
    }
    Ok(())
}
