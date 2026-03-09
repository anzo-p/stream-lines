use std::fmt;
use crate::errors::ProcessError;

impl fmt::Display for ProcessError {
    fn fmt(&self, f: &mut fmt::Formatter<'_>) -> fmt::Result {
        match self {
            ProcessError::AwsSdkError(msg) => write!(f, "AWS SDK error: {}", msg),
            ProcessError::EnvVarError(msg) => write!(f, "Environment variable error: {}", msg),
            ProcessError::UrlParseError(msg) => write!(f, "Url parse error: {}", msg),
        }
    }
}

impl std::error::Error for ProcessError {}
