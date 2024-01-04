use futures_util::{stream::StreamExt, SinkExt};
use lazy_static::lazy_static;
use serde::Serialize;
use std::cmp;
use std::{collections::HashMap, sync::Arc};
use tokio::net::TcpStream;
use tokio::sync::Mutex;
use tokio::sync::MutexGuard;
use tokio::time::{sleep, timeout, Duration};
use tokio_tungstenite::{connect_async, tungstenite::Message, MaybeTlsStream, WebSocketStream};
use url::Url;

use crate::error_handling::ProcessError;
use crate::load_app_config;
use crate::websocket::{AuthMessage, SubMessage};

const INITIAL_BACKOFF: u64 = 1;
const MAX_BACKOFF: u64 = 64;
const BACKOFF_FACTOR: u64 = 2;

lazy_static! {
    static ref WEBSOCKET_CONNECTIONS: Mutex<HashMap<String, Arc<Mutex<WebSocketStream<MaybeTlsStream<TcpStream>>>>>> =
        Mutex::new(HashMap::new());
}

pub async fn acquire_connection() -> Result<(), ProcessError> {
    let app_config = load_app_config()?;

    for feed in app_config.feeds {
        connect_and_sub(&feed.url, &feed.symbols, false).await?;
    }
    Ok(())
}

pub async fn read_from_connection(url_str: &str) -> Result<Option<Message>, ProcessError> {
    let connections = WEBSOCKET_CONNECTIONS.lock().await;
    let mut backoff = INITIAL_BACKOFF;

    if let Some(ws_arc) = connections.get(url_str) {
        loop {
            let mut ws: MutexGuard<_> = ws_arc.lock().await;
            match ws.next().await {
                Some(Ok(message)) => return Ok(Some(message)),

                other => {
                    let error_message = if let Some(Err(e)) = other {
                        format!("Websocket read error: {:?}, ", e)
                    } else {
                        String::from("Websocket connection closed, ")
                    };

                    eprintln!(
                        "{} - {}attempting to reconnect after {} seconds...",
                        chrono::Local::now(),
                        error_message,
                        backoff
                    );
                    drop(ws);
                    sleep(Duration::from_secs(backoff)).await;

                    eprintln!("{} - Reconnecting to {}", chrono::Local::now(), url_str);
                    let connect_result =
                        timeout(Duration::from_secs(MAX_BACKOFF), connect_to_stream(url_str, true)).await;

                    // still nort reconnecting after timeout and retry
                    match connect_result {
                        Ok(Ok(())) => {
                            eprintln!("Reconnected successfully.");
                            backoff = INITIAL_BACKOFF;

                            let app_config = load_app_config()?;

                            if let Some(feed) = app_config.feeds.iter().find(|f| f.url == url_str) {
                                connect_and_sub(url_str, &feed.symbols, true).await?;
                            }

                            return Ok(None);
                        }
                        Ok(Err(e)) => {
                            eprintln!("Error during reconnection: {:?}", e);
                            backoff = cmp::min(backoff.clone() * BACKOFF_FACTOR, MAX_BACKOFF);
                        }
                        Err(_) => {
                            eprintln!("Reconnection attempt timed out.");
                            backoff = cmp::min(backoff.clone() * BACKOFF_FACTOR, MAX_BACKOFF);
                        }
                    }
                }
            }
        }
    } else {
        Ok(None)
    }
}

async fn connect_and_sub(url_str: &str, trading_symbols: &[String], reconnect: bool) -> Result<(), ProcessError> {
    let _ = connect_to_stream(url_str, reconnect).await?;

    if let Err(e) = send_auth_message(url_str).await {
        return Err(e);
    }
    if let Err(e) = send_sub_message(url_str, trading_symbols).await {
        return Err(e);
    }
    Ok(())
}

async fn connect_to_stream(url_str: &str, reconnect: bool) -> Result<(), ProcessError> {
    let mut connections = WEBSOCKET_CONNECTIONS.lock().await;

    if !reconnect && connections.contains_key(url_str) {
        return Ok(());
    }

    let url = Url::parse(url_str).map_err(|e| ProcessError::UrlParseError(e))?;
    let (ws_stream, _) = connect_async(&url)
        .await
        .map_err(|e| ProcessError::WebSocketConnectionError {
            url: url.to_string(),
            source: e,
        })?;

    connections.insert(url_str.to_string(), Arc::new(Mutex::new(ws_stream)));
    Ok(())
}

async fn send_message<T: Serialize>(url_str: &str, message: &T) -> Result<(), ProcessError> {
    let content = serde_json::to_string(message)?;
    let ws_message = Message::Text(content).to_string();

    let connections = WEBSOCKET_CONNECTIONS.lock().await;
    if let Some(ws_arc) = connections.get(url_str) {
        let mut ws = ws_arc.lock().await;
        ws.send(Message::from(ws_message)).await?;
    }
    Ok(())
}

async fn send_sub_message(url_str: &str, trading_symbols: &[String]) -> Result<(), ProcessError> {
    let payload = SubMessage::new(&trading_symbols.to_vec())?;
    send_message(url_str, &payload).await
}

async fn send_auth_message(url_str: &str) -> Result<(), ProcessError> {
    let payload = AuthMessage::new()?;
    send_message(url_str, &payload).await
}
