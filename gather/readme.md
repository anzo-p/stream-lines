
## Gather - a data fetcher

A small exercise in Kotlin, Spring Boot, InfluxDB, DynamoDB in awssdk, and Webclient.

## Fetch financials from DataJockey

Acquires [financials, both annual and quarterly](https://datajockey.io) for the same companies that we obtain market data for. This allows to make all sorts of analytics to compare price development against actual business results. The analytics is planned to be calculated in a separate service running Apache Flink.

A free tier account is allowed to only make 10 calls per minute. Also as this data is expected to be static and update only with new data introduced quarterly. It doesnt need to run through all the tickers frequently. Instead we fetch a small subset of the companies taken as a shuffle with uniform probability.

<p align="center">
  <br>
  <img src="doc/images/stirling_approx.png" width="180" height="50" alt="Stirling approximation">
</p>

According to Stirling approximation it should take about the banking days of one month with hourly samples of 7 companies to shuffle through 200 tickers. There will be duplicate attempts for companies, and rightly should be as they publish new results every quarter. However, no state is required to manage the companies already fetched for.

## Fetch Historical prices from Alpaca

Acquires [historical bar data from Alpaca](https://docs.alpaca.markets/reference/stockbars-1) for the [n most valuable companies in the S&P 500 index](src/main/resources/tickers.yml). Those data are then used to compose an *equally weighted index with a daily rebalance*, whose evolution aims to mimic, though not to strictly follow, the daily features of the S&P 500. This effort potentially allows to search for [insights about trends in price development and money flows](influxdb/queries/example_queries.flux), and to discover curious patterns for possible trading signals. The service pays no attention to either dividends or brokering fees.

| from aggregated index value                 | to a breakdown of underlying development         |
|---------------------------------------------|--------------------------------------------------|
| ![](doc/images/index_trend_logarithmic.jpg) | ![](doc/images/securities_trend_logarithmic.jpg) |

### Equally weighted index

All securities start with the same weight. Securities that are added over time will be added at an equal weight to the index value at the start of that day.

### Daily rebalance

Adjusts the weights daily to maintain equal distribution among securities. All securities begin each day with equal opportunity to move the index while historical gains remain mostly affected by best performers.

### Redo individual stock after Split or Merge event

First of all, use query variable `adjustment='All'` in the bar data request https://data.alpaca.markets/v2/stocks/bars.

It might take few days before Alpaca has taken the stock event into account. After that the stock event will be automatically calculated into all prices.

Delete price data for that ticker from just before the event and the system will reftch the bar data with adjusted prices and recompute its effects on the index.

```
docker exec -it influxdb bash

influx delete --org <organisation> \
  --bucket <bucket> \
  --start <one full day before split event> \
  --stop '2099-12-31T23:59:59Z' \
  --predicate '_measurement="sec_raw_30mi" AND ticker=<ticker>' \
  --token <token>
```
