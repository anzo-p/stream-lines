<p align="center">
  <img src="doc/images/index_trend_logarithmic.jpg" alt="Logarithmic trend over value gain of index">
</p>

## Gather - a fetcher of historical bar data

A small exercise in Kotlin, Spring Boot, InfluxDB, DynamoDB in awssdk, and Webclient.

This service acquires [historical bar data from Alpaca](https://docs.alpaca.markets/reference/stockbars-1) for the [n most valuable companies in the S&P 500 index](src/main/resources/tickers.yml). Those data are then used to compose an *equally weighted index with a daily rebalance*, whose evolution aims to mimic, though not to strictly follow, the daily features of the S&P 500. This effort potentially allows to search for [insights about trends in price development and money flows](influxdb/queries/example_queries.flux), and to discover curious patterns for possible trading signals. The service pays no attention to either dividends or brokering fees.

### Equally weighted index

All securities start with the same weight. Securities that are added over time will be added at an equal weight to the index value at the start of that day.

### Daily rebalance

Adjusts the weights daily to maintain equal distribution among securities. All securities begin each day with equal opportunity to move the index while historical gains remain mostly affected by best performers.

<p align="center">
  <br>
  <img src="doc/images/securities_trend_logarithmic.jpg" alt="Logarithmic trend over value gain in various securities">
</p>