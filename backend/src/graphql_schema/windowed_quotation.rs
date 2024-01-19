use std::env;
use async_graphql::{Context, Error, InputObject, Object, SimpleObject};
use influxdb2::models::Query;
use influxdb2::FromDataPoint;
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

pub struct WindowedQuotationRoot {
    influx_client: influxdb2::Client,
    inxlux_bucket: String,
}

impl WindowedQuotationRoot {
    pub fn new(client: &influxdb2::Client) -> Self {
        let influx_bucket =
        env::var("INFLUXDB_BUCKET").map_err(|_| "INFLUXDB_BUCKET environment variable not found".to_string());

        WindowedQuotationRoot {
            influx_client: client.clone(),
            inxlux_bucket: influx_bucket.unwrap(),
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

        let query_str = format!(
            "from(bucket: \"{}\")
                |> range(start: {} , stop: {})
                |> filter(fn: (r) => r.symbol == \"{}\")
                ",
            self.inxlux_bucket, input.start_time, input.end_time, input.symbol
        );

        let query = Query::new(query_str);

        match self.influx_client.query::<>(Some(query)).await {
            Ok(fetch) if !fetch.is_empty() => Ok(fetch),
            _ => Ok(vec![]),
        }
    }
}
