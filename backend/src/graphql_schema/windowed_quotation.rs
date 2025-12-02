use async_graphql::{Context, Error, InputObject, Object, SimpleObject};
use influxdb2::models::Query;
use influxdb2::FromDataPoint;
use std::env;
use std::string::String;

#[derive(SimpleObject, FromDataPoint, Default)]
pub struct WindowedQuotationData {
    pub measure_id: String,
    pub measurement: String,
    pub symbol: String,
    pub window_start_time: f64,
    pub window_end_time: f64,
    pub sum_bid_volume: f64,
    pub sum_ask_volume: f64,
    pub record_count: f64,
    pub average_bid_price: f64,
    pub average_ask_price: f64,
    pub bid_price_at_window_end: f64,
    pub ask_price_at_window_end: f64,
}

#[derive(InputObject)]
pub struct WindowedQuotationQueryInput {
    pub symbol: String,
    pub start_time: i64,
    pub end_time: i64,
}

#[derive(InputObject)]
pub struct WindowedQuotationQueryInputLastHour {
    pub symbol: String,
}

pub struct WindowedQuotationRoot {
    influx_client: influxdb2::Client,
    influx_bucket: String,
}

impl WindowedQuotationRoot {
    pub fn new(client: &influxdb2::Client) -> Self {
        let influx_bucket =
            env::var("INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL")
                .map_err(|_| "INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL environment variable not found".to_string());

        WindowedQuotationRoot {
            influx_client: client.clone(),
            influx_bucket: influx_bucket.unwrap(),
        }
    }
}

#[Object]
#[allow(unused_variables)]
impl WindowedQuotationRoot {
    pub async fn get_windowed_quotation_data(
        &self,
        ctx: &Context<'_>,
        input: WindowedQuotationQueryInput,
    ) -> Result<Vec<WindowedQuotationData>, Error> {
        let query = query_influx(&self.influx_bucket, &input.symbol, input.start_time, input.end_time);

        match self.influx_client.query(Some(query)).await {
            Ok(fetch) if !fetch.is_empty() => Ok(fetch),
            _ => Ok(vec![]),
        }
    }

    pub async fn get_windowed_quotation_data_last_hour(
        &self,
        ctx: &Context<'_>,
        input: WindowedQuotationQueryInputLastHour,
    ) -> Result<Vec<WindowedQuotationData>, Error> {
        let query = query_influx(
            &self.influx_bucket,
            &input.symbol,
            chrono::Utc::now().timestamp() - (60 * 60),
            chrono::Utc::now().timestamp(),
        );

        match self.influx_client.query(Some(query)).await {
            Ok(fetch) if !fetch.is_empty() => Ok(fetch),
            _ => Ok(vec![]),
        }
    }
}

fn query_influx(bucket: &str, symbol: &str, start_time: i64, end_time: i64) -> Query {
    Query::new(format!(
        "from(bucket: \"{}\")
        |> range(start: {} , stop: {})
        |> filter(fn: (r) => r.symbol == \"{}\")
        ",
        bucket, start_time, end_time, symbol
    ))
}
