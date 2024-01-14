use std::env;

use crate::errors::ProcessError;

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
