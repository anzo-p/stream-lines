mod config;
mod errors;
mod http;
mod protobuf;
mod stream_producer;
mod types;
mod ws_connection;
mod ws_feed_consumer;

use log::info;
use signal_hook::consts::signal::SIGTERM;
use signal_hook::iterator::Signals;
use std::sync::atomic::{AtomicBool, Ordering};
use std::sync::Arc;
use std::thread;
use tokio::time::{sleep, Duration};

use crate::config::{logger, AppConfig};
use crate::errors::ProcessError;
//use crate::http::launch_health_server;
use crate::ws_connection::remove_active_connections;
use crate::ws_feed_consumer::run_one_feed;

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
    let feeds = app_config.feeds.clone();
    let mut tasks = Vec::new();

    for feed in feeds {
        let task = tokio::spawn(run_one_feed(feed, 5));
        tasks.push(task);
    }

    while running.load(Ordering::SeqCst) && !tasks.is_empty() {
        tasks.retain(|task| !task.is_finished());

        if tasks.is_empty() {
            info!("All feeds have stopped. Shutting down the application.");
            running.store(false, Ordering::SeqCst);
            break;
        }

        sleep(Duration::from_secs(5)).await;
    }
}

#[tokio::main]
async fn main() -> Result<(), ProcessError> {
    logger::init();
    info!("Application starting.");

    let running = Arc::new(AtomicBool::new(true));

    setup_sigterm_handler(running.clone());

    //tokio::spawn(launch_health_server());

    let app_config = load_app_config()?;
    while running.load(Ordering::SeqCst) {
        run_app(&app_config, running.clone()).await;
    }

    info!("Application shutting down.");
    remove_active_connections().await;
    Ok(())
}
