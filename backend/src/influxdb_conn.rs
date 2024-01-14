use influxdb2::Client;
use std::env;

pub async fn create_influxdb_client() -> Result<Client, String> {
    let db_url_str = env::var("INFLUXDB_URL").unwrap_or_else(|_| "http://localhost:8086".to_string());

    let influxdb_url =
        url::Url::parse(&db_url_str).map_err(|_| "Unable to parse INFLUXDB_URL environment variable".to_string())?;

    let organization =
        env::var("INFLUXDB_ORG").map_err(|_| "INFLUXDB_ORG environment variable not found".to_string())?;

    let token = env::var("INFLUXDB_TOKEN")
        .map_err(|_| "INFLUXDB_TOKEN environment variable not found".to_string())?
        .trim_matches('"')
        .to_string();

    let client = Client::new(influxdb_url, organization, token);

    Ok(client)
}
