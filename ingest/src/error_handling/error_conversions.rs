use aws_sdk_kinesis::error::SdkError;
use aws_sdk_kinesis::operation::put_record::PutRecordError;
use prost::EncodeError;
use std::env;

use crate::error_handling::ProcessError;

impl From<SdkError<PutRecordError>> for ProcessError {
    fn from(err: SdkError<PutRecordError>) -> Self {
        ProcessError::AwsSdkError(err.to_string())
    }
}

impl From<env::VarError> for ProcessError {
    fn from(err: env::VarError) -> Self {
        ProcessError::EnvVarError(err.to_string())
    }
}

impl From<EncodeError> for ProcessError {
    fn from(err: EncodeError) -> Self {
        ProcessError::ProtobufConversionError(err.to_string())
    }
}

impl From<serde_json::Error> for ProcessError {
    fn from(err: serde_json::Error) -> Self {
        ProcessError::SerializationError(err)
    }
}

impl From<tokio_tungstenite::tungstenite::Error> for ProcessError {
    fn from(err: tokio_tungstenite::tungstenite::Error) -> Self {
        ProcessError::WebSocketCommunicationError(err)
    }
}

impl From<url::ParseError> for ProcessError {
    fn from(err: url::ParseError) -> Self {
        ProcessError::UrlParseError(err)
    }
}
