package net.anzop.retro.service

import java.net.URI
import java.time.OffsetDateTime
import net.anzop.retro.config.AlpacaProps
import net.anzop.retro.config.tickerConfig.TickerConfig
import net.anzop.retro.helpers.buildHistoricalBarsUri
import net.anzop.retro.helpers.getRequest
import net.anzop.retro.helpers.resolveStartDate
import net.anzop.retro.repository.BarDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class BarDataFetcher(
    private val alpacaProps: AlpacaProps,
    private val tickerConfig: TickerConfig,
    private val barDataRepository: BarDataRepository,
    private val webClient: WebClient
) {
    private val logger = LoggerFactory.getLogger(BarDataFetcher::class.java)

    fun run() {
        run("1Day")
    }

    private fun run(measurement: String) =
        tickerConfig.tickers.forEach { ticker ->
            val startDate = getStartDate(ticker.symbol, measurement) ?: return@forEach
            processTicker(ticker.symbol, measurement, startDate)
        }

    private fun getStartDate(ticker: String, measurement: String): OffsetDateTime? {
        val lastKnownFetchDate = barDataRepository.getLatestMeasurementTime(ticker, measurement)
        logger.info("Last known fetch date for $ticker is $lastKnownFetchDate")

        val startDate = resolveStartDate(lastKnownFetchDate, alpacaProps.earliestHistoricalDate)
        logger.info("startDate was set to $startDate")
        return startDate
    }

    private fun handleResponse(ticker: String, response: BarsResponse?, measurement: String) =
        response?.let { body ->
            val n = body.bars.values.sumOf { it.size }
            when {
                n == 0 -> {
                    logger.warn("Response for historical bars from Alpaca is empty. Perhaps ticker: $ticker is invalid or outdated.")
                }
                else -> {
                    logger.info("Fetched $n bars for $ticker")
                    body.bars.forEach { (ticker, bars) ->
                        bars
                            .map { bar -> bar.toModel(ticker, measurement) }
                            .forEach(barDataRepository::save)
                    }
                }
            }
        }

    private tailrec fun processTicker(
        ticker: String,
        measurement: String,
        startDate: OffsetDateTime,
        pageToken: String = ""
    ) {
        throttle()

        val uri = buildHistoricalBarsUri(
            baseUrl = URI.create(alpacaProps.dailyBarsUrl),
            feed = alpacaProps.dataSource,
            symbols = listOf(ticker),
            timeframe = measurement,
            start = startDate,
            pageToken = pageToken
        )
        val response = webClient.getRequest<BarsResponse>(uri)
        handleResponse(ticker, response, measurement)

        response?.nextPageToken?.takeIf { it.isNotEmpty() }?.let { nextPage ->
            return processTicker(ticker, measurement, startDate, nextPage)
        }
    }

    fun throttle() {
        Thread.sleep((60000 / alpacaProps.maxCallsPerMinute).toLong())
    }
}
