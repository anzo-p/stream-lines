package net.anzop.retro.service

import java.net.URI
import java.time.OffsetDateTime
import net.anzop.retro.config.AlpacaProps
import net.anzop.retro.config.Tickers
import net.anzop.retro.helpers.buildUri
import net.anzop.retro.helpers.getRequest
import net.anzop.retro.helpers.resolveStartDate
import net.anzop.retro.repository.BarDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class BarDataFetcher(
    private val tickerConfig: Tickers,
    private val alpacaProps: AlpacaProps,
    private val webClient: WebClient,
    private val barDataRepository: BarDataRepository
) {

    private val logger = LoggerFactory.getLogger(BarDataFetcher::class.java)

    fun run() {
        run("1Day")
    }

    fun run(measurement: String) =
        tickerConfig.tickers.forEach { ticker ->
            throttle()

            val startDate = resolveStartDate(ticker, measurement) ?: return@forEach
            processTicker(ticker, measurement, startDate)
        }

    private fun resolveStartDate(ticker: String, measurement: String): OffsetDateTime? {
        val lastKnownFetchDate = barDataRepository.getLatestMeasurementTime(ticker, measurement)
        logger.info("Last known fetch date for $ticker is $lastKnownFetchDate")

        val startDate = resolveStartDate(lastKnownFetchDate, alpacaProps.earliestHistoricalDate)
        logger.info("Fetching data for $ticker from $startDate")
        return startDate
    }

    private fun handleResponse(response: BarsResponse?, measurement: String) =
        response?.let { body ->
            logger.info("Fetched ${body.bars.values.sumOf { it.size }} items")

            body.bars.forEach { (ticker, bars) ->
                bars.map { bar -> bar.toModel(ticker, measurement) }
                    .forEach(barDataRepository::save)
            }
        }

    private tailrec fun processTicker(
        ticker: String,
        measurement: String,
        startDate: OffsetDateTime,
        pageToken: String = ""
    ) {
        throttle()

        val uri = buildUri(
            baseUrl = URI.create(alpacaProps.dailyBarsUrl),
            feed = alpacaProps.dataSource,
            symbols = listOf(ticker),
            timeframe = measurement,
            start = startDate,
            pageToken = pageToken
        )
        val response = webClient.getRequest<BarsResponse>(uri)
        handleResponse(response, measurement)

        response?.nextPageToken?.takeIf { it.isNotEmpty() }?.let { nextPage ->
            return processTicker(ticker, measurement, startDate, nextPage)
        }
    }

    fun throttle() {
        Thread.sleep((60000 / alpacaProps.maxCallsPerMinute).toLong())
    }
}
