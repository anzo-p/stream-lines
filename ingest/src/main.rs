use chrono::{DateTime, FixedOffset};
use dotenv;
use futures_util::{SinkExt, stream::StreamExt};
use rust_decimal::Decimal;
use serde::{Deserialize, Serialize};
use serde_json::{self, Value};
use std::env;
use tokio::net::TcpStream;
use tokio_tungstenite::{connect_async, tungstenite::protocol::Message, MaybeTlsStream, WebSocketStream};
use url::Url;

#[derive(Serialize, Deserialize)]
struct AuthMessage {
    action: String,
    key: String,
    secret: String
}

#[derive(Serialize, Deserialize)]
struct SubMessage {
    action: String,
    trades: Vec<String>,
    quotes: Vec<String>
}

#[derive(Serialize, Deserialize, Debug)]
struct QuotationMessage {
    #[serde(rename = "T")] message_type: String,
    #[serde(rename = "S")] symbol: String,
    #[serde(rename = "bx")] bid_exchange: String,
    #[serde(rename = "bp")] bid_price: Decimal,
    #[serde(rename = "bs")] bid_size: i32,
    #[serde(rename = "ax")] ask_exchange: String,
    #[serde(rename = "ap")] ask_price: Decimal,
    #[serde(rename = "as")] ask_size: i32,
    #[serde(rename = "t")] market_timestamp: DateTime<FixedOffset>,
    #[serde(rename = "c")] conditions: Vec<String>,
    #[serde(rename = "z")] tape: String
}

#[derive(Serialize, Deserialize, Debug)]
struct TradeMessage {
    #[serde(rename = "T")] message_type: String,
    #[serde(rename = "S")] symbol: String,
    #[serde(rename = "i")] trade_id: i32,
    #[serde(rename = "x")] exchange: String,
    #[serde(rename = "p")] price: Decimal,
    #[serde(rename = "s")] size: i32,
    #[serde(rename = "c")] conditions: Vec<String>,
    #[serde(rename = "t")] market_timestamp: DateTime<FixedOffset>,
    #[serde(rename = "z")] tape: String,
}

#[derive(Debug)]
enum ProcessError {
    EnvVarError(String),
    ParsingError { msg: String, item_type: String },
    SerializationError(serde_json::Error),
    WebSocketConnectionError { url: String, source: tokio_tungstenite::tungstenite::Error },
    WebSocketCommunicationError(tokio_tungstenite::tungstenite::Error),
    WebSocketReadError(tokio_tungstenite::tungstenite::Error),
    UnknownItemType(Value),
    UrlParseError(url::ParseError),
}

impl From<serde_json::Error> for ProcessError {
    fn from(err: serde_json::Error) -> Self {
        ProcessError::SerializationError(err)
    }
}

impl From<tokio_tungstenite::tungstenite::Error> for ProcessError {
    fn from(err: tokio_tungstenite::tungstenite::Error) -> Self {
        ProcessError::WebSocketCommunicationError(err)
    }
}

impl From<env::VarError> for ProcessError {
    fn from(err: env::VarError) -> Self {
        ProcessError::EnvVarError(err.to_string())
    }
}

impl From<url::ParseError> for ProcessError {
    fn from(err: url::ParseError) -> Self {
        ProcessError::UrlParseError(err)
    }
}

fn handle_process_error(e: &ProcessError) {
    match e {
        ProcessError::EnvVarError(msg) => eprintln!("Environment variable error: {}", msg),
        ProcessError::ParsingError { msg, item_type } => eprintln!("Error parsing {}: {}", item_type, msg),
        ProcessError::SerializationError(_) => eprintln!("Serialization error"),
        ProcessError::WebSocketConnectionError { url, source } => eprintln!("Failed to connect to WebSocket server at {}: {}", url, source),
        ProcessError::WebSocketCommunicationError(e) => eprintln!("WebSocket communication error occurred: {}", e),
        ProcessError::WebSocketReadError(err) => eprintln!("WebSocket read error: {}", err),
        ProcessError::UnknownItemType(item) => eprintln!("Unknown message type: {:?}", item),
        ProcessError::UrlParseError(msg) => eprintln!("Url parse error: {}", msg)
    }
}

async fn process_item(item: Value) -> Result<(), ProcessError> {
    match item.get("T").and_then(Value::as_str) {
        Some("q") => {
            let quotation_message: QuotationMessage = serde_json::from_value(item)
                .map_err(|e| ProcessError::ParsingError {
                    msg: e.to_string(),
                    item_type: "QuotationMessage".to_string()
                })?;

            // kinesis write
            println!("Quotation: {:?}", quotation_message);
        },

        Some("t") => {
            let trade_message: TradeMessage = serde_json::from_value(item)
                .map_err(|e| ProcessError::ParsingError {
                    msg: e.to_string(),
                    item_type: "TradeMessage".to_string()
                })?;

            // kinesis write
            println!("Trade: {:?}", trade_message);
        },

        // normal operation log
        Some("success") => println!("{:?}", item.get("msg")),

        Some("subscription") => println!("successful subscription to market data: {:?}", item.to_string()),

        _ => return Err(ProcessError::UnknownItemType(item.clone())),
    }

    Ok(())
}

async fn loop_market_data(values: Value) -> Result<(), ProcessError> {
    if let Value::Array(array) = values {
        for item in array {
            process_item(item).await?
        }
    }
    Ok(())
}

async fn process_message(ws_message: Message) -> Result<(), ProcessError> {
    match ws_message {
        Message::Text(text) => {
            let json_value: Value = serde_json::from_str(&text)?;
            loop_market_data(json_value).await?;
            Ok(())
        }

        _ => {
            // log or error
            println!("Received non-text message");
            Ok(())
        }
    }
}

async fn send_message<T: Serialize>(ws_stream: &mut WebSocketStream<MaybeTlsStream<TcpStream>>, message: &T) -> Result<(), ProcessError> {
    let ws_message = serde_json::to_string(message)?;
    ws_stream.send(Message::Text(ws_message)).await?;
    Ok(())
}

async fn send_sub_message(ws_stream: &mut WebSocketStream<MaybeTlsStream<TcpStream>>) -> Result<(), ProcessError> {
    let payload = SubMessage {
        action: "subscribe".to_string(),
        trades: vec!["MSFT".to_string()],
        quotes: vec!["MSFT".to_string()]
    };
    send_message(ws_stream, &payload).await
}

async fn send_auth_message(ws_stream: &mut WebSocketStream<MaybeTlsStream<TcpStream>>) -> Result<(), ProcessError> {
    let payload = AuthMessage {
        action: "auth".to_string(),
        key: env::var("API_KEY")?,
        secret: env::var("API_SECRET")?,
    };
    send_message(ws_stream, &payload).await
}

async fn connect_to_feed() -> Result<WebSocketStream<MaybeTlsStream<TcpStream>>, ProcessError> {
    let url_str = env::var("MARKET_DATA_URL")?;
    let url = Url::parse(&*url_str)?;
    let (ws_stream, _) = connect_async(&url)
        .await
        .map_err(|e| ProcessError::WebSocketConnectionError {
            url: url.to_string(),
            source: e
        })?;

    Ok(ws_stream)
}

#[tokio::main]
async fn main() {
    dotenv::dotenv().ok();

    match connect_to_feed().await {
        Ok(mut ws_stream) => {
            if let Err(e) = send_auth_message(&mut ws_stream).await {
                handle_process_error(&e);
            }
            if let Err(e) = send_sub_message(&mut ws_stream).await {
                handle_process_error(&e);
            }

            let (_, mut read) = ws_stream.split();

            while let Some(message) = read.next().await {
                match message {
                    Ok(msg) => {
                        if let Err(e) = process_message(msg).await {
                            handle_process_error(&e);
                        }
                    }

                    Err(e) => {
                        handle_process_error(&ProcessError::WebSocketReadError(e));
                        break;
                    }
                }
            }
        },

        Err(e) => {
            handle_process_error(&e);
        }
    }
}