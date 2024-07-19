package net.anzop.retro.service

import java.net.URI
import java.time.OffsetDateTime
import net.anzop.retro.config.AlpacaProps
import net.anzop.retro.config.tickerConfig.TickerConfig
import net.anzop.retro.helpers.buildHistoricalBarsUri
import net.anzop.retro.helpers.getRequest
import net.anzop.retro.helpers.toInstant
import net.anzop.retro.helpers.toOffsetDateTime
import net.anzop.retro.http.client.BarDataDto
import net.anzop.retro.http.client.BarsResponse
import net.anzop.retro.model.marketData.Measurement
import net.anzop.retro.repository.dynamodb.CacheRepository
import net.anzop.retro.repository.influxdb.MarketDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class BarDataFetcher(
    private val alpacaProps: AlpacaProps,
    private val tickerConfig: TickerConfig,
    private val cacheRepository: CacheRepository,
    private val marketDataRepository: MarketDataRepository,
    private val webClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(BarDataFetcher::class.java)

    fun run() {
        val earliestMarketTimestamp = tickerConfig
            .tickers
            .mapNotNull { ticker ->
                val startDate = resolveStartDate(ticker.symbol)
                processTicker(ticker.symbol, startDate)
            }
            .min()

        logger.info("The earliest processed marketTimestamp was $earliestMarketTimestamp")
        earliestMarketTimestamp.let {
            cacheRepository.suggestIndexStaleFrom(it.toLocalDate())
        }
    }

    private fun resolveStartDate(ticker: String): OffsetDateTime {
        val latestMarketTimestamp = marketDataRepository.getEarliestSourceBarDataEntry(ticker)
        logger.info("Last known marketTimestamp for $ticker is $latestMarketTimestamp")

        val startDate = (latestMarketTimestamp?: alpacaProps.earliestHistoricalDate.toInstant()).toOffsetDateTime()
        logger.info("startDate was set to $startDate")
        return startDate
    }

    private tailrec fun processTicker(
        ticker: String,
        startDate: OffsetDateTime,
        pageToken: String = "",
        accFirstEntry: OffsetDateTime? = null
    ): OffsetDateTime? {
        throttle()

        val uri = buildHistoricalBarsUri(
            baseUrl = URI.create(alpacaProps.dailyBarsUrl),
            feed = alpacaProps.dataSource,
            symbols = listOf(ticker),
            timeframe = alpacaProps.barDataTimeframe,
            start = startDate,
            pageToken = pageToken
        )
        val response = webClient.getRequest<BarsResponse>(uri)
        val firstEntry = handleResponse(ticker, response)

        val newFirstEntry = if (accFirstEntry == null || (firstEntry != null && firstEntry < accFirstEntry)) {
            firstEntry
        } else {
            accFirstEntry
        }

        return if (response?.nextPageToken?.isNotEmpty() == true) {
            processTicker(ticker, startDate, response.nextPageToken!!, newFirstEntry)
        } else {
            newFirstEntry
        }
    }

    private fun handleResponse(ticker: String, response: BarsResponse?): OffsetDateTime? =
        response?.let { body ->
            val bars = body.bars
            val n = bars.values.sumOf { it.size }

            when {
                n == 0 -> {
                    logger.warn("Response from Alpaca is empty. Perhaps ticker: $ticker is invalid or outdated.")
                    null
                }
                else -> {
                    logger.info("Fetched $n bars for $ticker up to ${bars.values.flatten().last().marketTimestamp}")
                    processEntries(ticker, bars.values.flatten())
                }
            }
        }

    private fun processEntries(ticker: String, barEntries: List<BarDataDto>): OffsetDateTime {
        val bars = barEntries
            .mapNotNull { entry ->
                runCatching {
                    entry.toModel(Measurement.SECURITIES_RAW_DAILY, ticker)
                }.onFailure {
                    logger.warn("Validation failed for $ticker bar data: ${it.message}")
                }.getOrNull()
            }

        marketDataRepository.save(bars)

        return bars
            .minOf { it.marketTimestamp }
            .toOffsetDateTime()
    }

    private fun throttle() {
        Thread.sleep((60000 / alpacaProps.maxCallsPerMinute).toLong())
    }
}
