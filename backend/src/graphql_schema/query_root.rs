use async_graphql::{Context, EmptyMutation, EmptySubscription, Object, Schema};
use influxdb2::Client as InfluxClient;

use crate::graphql_schema::windowed_quotation::{
    WindowedQuotationData, WindowedQuotationQueryInput, WindowedQuotationRoot,
};

pub struct QueryRoot {
    influx_client: InfluxClient,
}

#[Object]
impl QueryRoot {
    async fn get_windowed_quotation_data(
        &self,
        ctx: &Context<'_>,
        input: WindowedQuotationQueryInput,
    ) -> Result<Vec<WindowedQuotationData>, async_graphql::Error> {
        let windowed_quotation_root = WindowedQuotationRoot::new(&self.influx_client);
        windowed_quotation_root.get_windowed_quotation_data(ctx, input).await
    }
}

pub type MySchema = Schema<QueryRoot, EmptyMutation, EmptySubscription>;

pub fn create_schema(influx_client: InfluxClient) -> MySchema {
    let query_root = QueryRoot { influx_client };
    Schema::new(query_root, EmptyMutation, EmptySubscription)
}