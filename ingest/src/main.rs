mod app_config;
mod error_handling;
mod protobuf;
mod shared_types;
mod stream_producer;
mod websocket;

use dotenv;
use signal_hook::consts::signal::SIGTERM;
use signal_hook::iterator::Signals;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;
use std::thread;

use crate::app_config::AppConfig;
use crate::error_handling::ProcessError;
use crate::websocket::{run_feeds, send_closing_messages_and_shutdown};

fn load_app_config() -> Result<AppConfig, ProcessError> {
    AppConfig::new().map_err(|e| ProcessError::ConfigError(e.to_string()))
}

fn setup_sigterm_handler(running: Arc<AtomicBool>) {
    thread::spawn(move || {
        let mut signals = Signals::new(&[SIGTERM]).unwrap();
        for _ in signals.forever() {
            running.store(false, Ordering::SeqCst);
        }
    });
}

async fn run_app(app_config: &AppConfig, running: Arc<AtomicBool>) {
    while running.load(Ordering::SeqCst) {
        if let Err(e) = run_feeds(&app_config).await {
            eprintln!("Error running feeds: {}", e);
        }
    }
}

#[tokio::main]
async fn main() -> Result<(), ProcessError> {
    dotenv::dotenv().ok();

    let running = Arc::new(AtomicBool::new(true));

    setup_sigterm_handler(running.clone());

    while running.load(Ordering::SeqCst) {
        let app_config = load_app_config()?;
        run_app(&app_config, running.clone()).await;
    }

    println!("Application shutting down gracefully.");
    send_closing_messages_and_shutdown().await;
    Ok(())
}
