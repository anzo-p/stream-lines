package net.anzop.retro.service

import java.net.URI
import java.time.OffsetDateTime
import net.anzop.retro.config.AlpacaProps
import net.anzop.retro.config.tickerConfig.TickerConfig
import net.anzop.retro.helpers.buildHistoricalBarsUri
import net.anzop.retro.helpers.getRequest
import net.anzop.retro.helpers.toInstant
import net.anzop.retro.helpers.toOffsetDateTime
import net.anzop.retro.http.client.BarsResponse
import net.anzop.retro.model.marketData.Measurement
import net.anzop.retro.repository.MarketDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class BarDataFetcher(
    private val alpacaProps: AlpacaProps,
    private val tickerConfig: TickerConfig,
    private val marketDataRepository: MarketDataRepository,
    private val webClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(BarDataFetcher::class.java)

    fun run() =
        tickerConfig.tickers.forEach { ticker ->
            val startDate = resolveStartDate(ticker.symbol)
            processTicker(ticker.symbol, startDate)
        }

    private fun resolveStartDate(ticker: String): OffsetDateTime {
        val latestMarketTimestamp = marketDataRepository.getLatestMeasurementTime(Measurement.SECURITIES_RAW_DAILY, ticker)
        logger.info("Last known marketTimestamp for $ticker is $latestMarketTimestamp")

        val startDate = (latestMarketTimestamp?: alpacaProps.earliestHistoricalDate.toInstant()).toOffsetDateTime()
        logger.info("startDate was set to $startDate")
        return startDate
    }

    private tailrec fun processTicker(
        ticker: String,
        startDate: OffsetDateTime,
        pageToken: String = ""
    ) {
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
        handleResponse(ticker, response)

        response
            ?.nextPageToken
            ?.takeIf { it.isNotEmpty() }
            ?.let { nextPage ->
                return processTicker(ticker, startDate, nextPage)
            }
    }

    private fun handleResponse(ticker: String, response: BarsResponse?) =
        response?.let { body ->
            val bars = body.bars
            val n = bars.values.sumOf { it.size }
            when {
                n == 0 -> {
                    logger.warn("Response from Alpaca is empty. Perhaps ticker: $ticker is invalid or outdated.")
                }
                else -> {
                    logger.info("Fetched $n bars for $ticker up to ${bars.values.flatten().last().marketTimestamp}")
                    val barDataList = bars.flatMap { entry ->
                        entry.value.mapNotNull { barDataDto ->
                            runCatching {
                                barDataDto.toModel(Measurement.SECURITIES_RAW_DAILY, entry.key)
                            }.onFailure {
                                logger.warn("Validation failed for $ticker bar data: ${it.message}")
                            }.getOrNull()
                        }
                    }
                    if (barDataList.isNotEmpty()) {
                        marketDataRepository.save(barDataList)
                    }
                }
            }
        }

    private fun throttle() {
        Thread.sleep((60000 / alpacaProps.maxCallsPerMinute).toLong())
    }
}
