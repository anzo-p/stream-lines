mod errors;
mod server;

use crate::errors::ProcessError;
use crate::server::start_server;

#[tokio::main]
async fn main() {
    start_server().await;
}
