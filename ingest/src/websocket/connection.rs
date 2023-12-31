use futures_util::{stream::StreamExt, SinkExt};
use lazy_static::lazy_static;
use serde::Serialize;
use std::{collections::HashMap, sync::Arc};
use tokio::net::TcpStream;
use tokio::sync::Mutex;
use tokio_tungstenite::{connect_async, tungstenite::Message, MaybeTlsStream, WebSocketStream};
use url::Url;

use crate::error_handling::ProcessError;
use crate::websocket::{AuthMessage, SubMessage};

lazy_static! {
    static ref WEBSOCKET_CONNECTIONS: Mutex<HashMap<String, Arc<Mutex<WebSocketStream<MaybeTlsStream<TcpStream>>>>>> =
        Mutex::new(HashMap::new());
}

pub async fn acquire_connection(url_str: &str, trading_symbols: &Vec<String>, reconnect: bool) -> Result<(), ProcessError> {
    let _ = connect_to_stream(url_str, reconnect).await?;

    if let Err(e) = send_auth_message(url_str).await {
        return Err(e);
    }
    if let Err(e) = send_sub_message(url_str, trading_symbols).await {
        return Err(e);
    }
    Ok(())
}

pub async fn read_from_connection(url_str: &str) -> Result<Option<Message>, ProcessError> {
    let connections = WEBSOCKET_CONNECTIONS.lock().await;
    if let Some(ws_arc) = connections.get(url_str) {
        let mut ws = ws_arc.lock().await;
        match ws.next().await {
            Some(Ok(message)) => Ok(Some(message)),
            Some(Err(e)) => {
                drop(ws);
                //connect_to_stream(url_str, true).await?;
                Err(ProcessError::WebSocketReadError(e))
            }
            None => Ok(None),
        }
    } else {
        Ok(None)
    }
}

async fn send_to_connection(url_str: &str, message: String) -> Result<(), ProcessError> {
    let connections = WEBSOCKET_CONNECTIONS.lock().await;
    if let Some(ws_arc) = connections.get(url_str) {
        let mut ws = ws_arc.lock().await;
        ws.send(Message::from(message)).await?;
    }
    Ok(())
}

async fn connect_to_stream(url_str: &str, reconnect: bool) -> Result<(), ProcessError> {
    let mut connections = WEBSOCKET_CONNECTIONS.lock().await;

    if !reconnect && connections.contains_key(url_str) {
        return Ok(());
    }

    let url = Url::parse(url_str).map_err(|e| ProcessError::UrlParseError(e))?;
    let (ws_stream, _) = connect_async(&url).await.map_err(|e| ProcessError::WebSocketConnectionError {
        url: url.to_string(),
        source: e,
    })?;

    connections.insert(url_str.to_string(), Arc::new(Mutex::new(ws_stream)));
    Ok(())
}

async fn send_message<T: Serialize>(url_str: &str, message: &T) -> Result<(), ProcessError> {
    let ws_message = serde_json::to_string(message)?;
    println!("Sending message: {}", ws_message);
    send_to_connection(url_str, Message::Text(ws_message).to_string()).await?;
    Ok(())
}

async fn send_sub_message(url_str: &str, trading_symbols: &Vec<String>) -> Result<(), ProcessError> {
    let payload = SubMessage::new(trading_symbols)?;
    send_message(url_str, &payload).await
}

async fn send_auth_message(url_str: &str) -> Result<(), ProcessError> {
    let payload = AuthMessage::new()?;
    send_message(url_str, &payload).await
}
