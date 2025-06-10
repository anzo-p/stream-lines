package net.anzop.gather.service

import java.net.URI
import java.time.OffsetDateTime
import net.anzop.gather.config.AlpacaProps
import net.anzop.gather.config.tickerConfig.TickerConfig
import net.anzop.gather.dto.bars.BarDataDto
import net.anzop.gather.dto.bars.BarsResponse
import net.anzop.gather.helpers.date.asAmericaNyToInstant
import net.anzop.gather.helpers.date.toOffsetDateTime
import net.anzop.gather.http.client.WebFluxExtensions.getRequest
import net.anzop.gather.http.client.buildHistoricalBarsUri
import net.anzop.gather.model.Ticker
import net.anzop.gather.model.marketData.Measurement
import net.anzop.gather.repository.dynamodb.IndexStaleRepository
import net.anzop.gather.repository.influxdb.MarketDataFacade
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class BarDataFetcher(
    private val alpacaProps: AlpacaProps,
    private val tickerConfig: TickerConfig,
    private val indexStaleRepository: IndexStaleRepository,
    private val marketDataFacade: MarketDataFacade,
    private val webClient: WebClient
) : ThrottlingFetcher(alpacaProps.maxCallsPerMinute) {

    private val logger = LoggerFactory.getLogger(BarDataFetcher::class.java)

    fun run() {
        val earliestMarketTimestamp = tickerConfig
            .tickers
            .mapNotNull { ticker ->
                val startDateTime = resolveStartDate(ticker.symbol)
                processTicker(ticker, startDateTime)
            }
            .min()

        logger.info("The earliest processed marketTimestamp was $earliestMarketTimestamp")
        earliestMarketTimestamp.let {
            indexStaleRepository.suggestIndexStaleFrom(it.toLocalDate())
        }
    }

    private fun resolveStartDate(ticker: String): OffsetDateTime {
        val startDate = marketDataFacade
            // re-fetching latest bar reveals that the ticker in url param still valid
            .getLatestSourceBarDataEntry(ticker)
            .also { logger.info("Last known marketTimestamp for $ticker is $it") }
            ?: alpacaProps
                .earliestHistoricalDate
                .asAmericaNyToInstant()
                .also { logger.info("No db entry for $ticker, defaulting to alpaca earliest at $it") }

        return startDate
            .toOffsetDateTime()
            .also { logger.info("startDate was set to $it") }
    }

    private tailrec fun processTicker(
        ticker: Ticker,
        startDateTime: OffsetDateTime,
        pageToken: String = "",
        accFirstEntry: OffsetDateTime? = null
    ): OffsetDateTime? {
        val uri = buildHistoricalBarsUri(
            baseUrl = URI.create(alpacaProps.dailyBarsUrl),
            feed = alpacaProps.dataSource,
            symbols = listOf(ticker.symbol),
            timeframe = alpacaProps.barDataTimeframe,
            start = startDateTime,
            pageToken = pageToken
        )
        val response = fetch { webClient.getRequest<BarsResponse>(uri) }
        val firstEntry = handleResponse(ticker, response)

        val newFirstEntry = if (accFirstEntry == null || (firstEntry != null && firstEntry < accFirstEntry)) {
            firstEntry
        } else {
            accFirstEntry
        }

        return if (response?.nextPageToken?.isNotEmpty() == true) {
            processTicker(ticker, startDateTime, response.nextPageToken!!, newFirstEntry)
        } else {
            newFirstEntry
        }
    }

    private fun handleResponse(ticker: Ticker, response: BarsResponse?): OffsetDateTime? =
        response?.let { body ->
            val bars = body.bars
            val n = bars.values.sumOf { it.size }

            when {
                n == 0 -> {
                    logger.warn("Response from Alpaca is empty. Perhaps ticker: " +
                            "${ticker.symbol} is invalid or outdated.")
                    null
                }
                else -> {
                    logger.info("Fetched $n bars for ${ticker.symbol} " +
                            "up to ${bars.values.flatten().last().marketTimestamp}")
                    processEntries(ticker, bars.values.flatten())
                }
            }
        }

    private fun processEntries(ticker: Ticker, barEntries: List<BarDataDto>): OffsetDateTime {
        val bars = barEntries
            .mapNotNull { entry ->
                runCatching {
                    entry.toModel(Measurement.SECURITY_RAW_SEMI_HOURLY, ticker)
                }.onFailure {
                    logger.warn("Validation failed for $ticker bar data: ${it.message}")
                }.getOrNull()
            }

        marketDataFacade.save(bars)

        return bars
            .minOf { it.marketTimestamp }
            .toOffsetDateTime()
    }
}
