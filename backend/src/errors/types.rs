#[derive(Debug)]
pub enum ProcessError {
    AwsSdkError(String),
    EnvVarError(String),
    UrlParseError(url::ParseError),
}
