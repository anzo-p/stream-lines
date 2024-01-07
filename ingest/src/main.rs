mod app_config;
mod error_handling;
mod protobuf;
mod shared_types;
mod stream_producer;
mod websocket;

use dotenv;
use signal_hook::consts::signal::SIGTERM;
use signal_hook::iterator::Signals;
use std::sync::{Arc, atomic::{AtomicBool, Ordering}};
use std::thread;
use tokio::time::{sleep, Duration};


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
    let mut retry_count = 0;
    let max_retries = 5;
    let retry_delay = Duration::from_secs(15);

    while running.load(Ordering::SeqCst) && retry_count < max_retries {
        match run_feeds(&app_config).await {
            Ok(_) => {
                retry_count = 0;
            },
            Err(e) => {
                retry_count += 1;
                let retry_count_str = retry_count.to_string() + "/" + max_retries.to_string().as_str();
                eprintln!("Error running feeds: {}. Retrying {} in {} seconds...", e, retry_count_str, retry_delay.as_secs());
                sleep(retry_delay).await;
            },
        }
    }

    if retry_count >= max_retries {
        eprintln!("Maximum retry attempts reached. Stopping application.");
        running.store(false, Ordering::SeqCst);
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
