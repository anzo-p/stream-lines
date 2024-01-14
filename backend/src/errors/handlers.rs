use std::fmt;

use crate::ProcessError;

pub fn handle_process_error(e: &ProcessError) {
    match e {
        ProcessError::EnvVarError(msg) => eprintln!("Environment variable error: {}", msg),
        ProcessError::UrlParseError(msg) => eprintln!("Url parse error: {}", msg),
    }
}

impl fmt::Display for ProcessError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ProcessError::EnvVarError(msg) => write!(f, "Environment variable error: {}", msg),
            ProcessError::UrlParseError(msg) => write!(f, "Url parse error: {}", msg),
        }
    }
}

impl std::error::Error for ProcessError {}
