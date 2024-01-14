use aws_config::BehaviorVersion;
use aws_sdk_kinesis::primitives::Blob;
use aws_sdk_kinesis::Client;
use std::env;

use crate::errors::ProcessError;

pub async fn create_kinesis_client() -> Result<Client, ProcessError> {
    let config = aws_config::load_defaults(BehaviorVersion::v2023_11_09()).await;
    let client = Client::new(&config);
    check_kinesis(client.clone()).await?;
    Ok(client)
}

pub async fn send_to_kinesis(client: &Client, key: &str, data: Vec<u8>) -> Result<(), ProcessError> {
    client
        .put_record()
        .data(Blob::new(data))
        .partition_key(key)
        .stream_name(get_stream_name()?)
        .send()
        .await?;

    Ok(())
}

async fn check_kinesis(client: Client) -> Result<(), ProcessError> {
    let stream_name = get_stream_name()?;
    let resp = client.describe_stream().stream_name(stream_name).send().await;

    match resp {
        Ok(_) => Ok(()),
        Err(err) => Err(ProcessError::AwsSdkError(err.to_string())),
    }
}

fn get_stream_name() -> Result<String, ProcessError> {
    env::var("KINESIS_UPSTREAM_NAME")
        .map_err(|_| ProcessError::EnvVarError(String::from("KINESIS_UPSTREAM_NAME not found in environment")))
}
