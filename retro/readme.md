## Retro - a fetcher of historical bar data

A small exercise in Kotlin, Spring Boot, InfluxDB, and Webclient.

This service acquires [historical bar data from Alpaca](https://docs.alpaca.markets/reference/stockbars-1) for the [n most valuable companies in the S&P 500 index](src/main/resources/tickers.yml). Those data are then used to compose an equally weighted index, whose evolution aims to mimic that of the S&P 500. Hopefully this effort allows to make insights about price development trends and money flows, and ultimately discover curious and abnormal trading signals.

The service runs only locally for now and is not yet part of the cloud deployment nor connects to cloud assets of the larger stream-lines project.
