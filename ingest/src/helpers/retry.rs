use rand::{thread_rng, Rng};
use std::future::Future;
use tokio::time::Duration;

use crate::errors::ProcessError;

const MAX_RETRIES: u32 = 7;
const RETRY_BASE: f64 = 2.0;
const JITTER_MIN: f64 = 0.75;
const JITTER_MAX: f64 = 1.2;

pub async fn retry_with_backoff<F, Fut, T>(f: F, f_name: &str) -> Result<T, ProcessError>
where
    F: Fn() -> Fut,
    Fut: Future<Output = Result<T, ProcessError>>,
{
    let mut retry_count = 0;

    while retry_count < MAX_RETRIES {
        match f().await {
            Ok(result) => return Ok(result),
            Err(_) => {
                retry_count += 1;
                log::warn!(
                    "Retrying operation {}. Attempt {} / {}",
                    f_name,
                    retry_count,
                    MAX_RETRIES
                );
                tokio::time::sleep(Duration::from_millis(compute_backoff_millis(&retry_count))).await;
            }
        }
    }

    Err(ProcessError::MaxRetriesReached(f_name.to_string()))
}

fn compute_backoff_millis(retry_count: &u32) -> u64 {
    let exponential_backoff = RETRY_BASE.powf(*retry_count as f64) * 1000.0;
    let jitter_factor: f64 = thread_rng().gen_range(JITTER_MIN..JITTER_MAX);

    (exponential_backoff * jitter_factor) as u64
}
