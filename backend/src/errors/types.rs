#[derive(Debug)]
pub enum ProcessError {
    EnvVarError(String),
    UrlParseError(url::ParseError),
}
