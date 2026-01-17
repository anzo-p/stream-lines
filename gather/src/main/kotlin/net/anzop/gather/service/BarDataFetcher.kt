package net.anzop.gather.service

import java.net.URI
import java.time.Duration
import java.time.Instant
import java.time.OffsetDateTime
import net.anzop.gather.config.AlpacaProps
import net.anzop.gather.config.SourceDataConfig
import net.anzop.gather.dto.bars.BarDataDto
import net.anzop.gather.dto.bars.BarsResponse
import net.anzop.gather.helpers.date.asAmericaNyToInstant
import net.anzop.gather.helpers.date.toOffsetDateTime
import net.anzop.gather.http.client.WebFluxExtensions.getRequest
import net.anzop.gather.http.client.buildGetHistoricalBarsUri
import net.anzop.gather.model.SourceDataParams
import net.anzop.gather.model.marketData.Measurement
import net.anzop.gather.repository.dynamodb.IndexStaleRepository
import net.anzop.gather.repository.influxdb.MarketDataFacade
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class BarDataFetcher(
    private val alpacaProps: AlpacaProps,
    private val sourceDataConfig: SourceDataConfig,
    private val indexStaleRepository: IndexStaleRepository,
    private val marketDataFacade: MarketDataFacade,
    private val alpacaWebClient: WebClient
) : ThrottlingFetcher(alpacaProps.maxCallsPerMinute) {

    private val logger = LoggerFactory.getLogger(BarDataFetcher::class.java)

    fun run(): Boolean {
        val start = Instant.now()
        val maxDuration = Duration.ofMinutes(alpacaProps.maxExecDurationMinutes)

        val earliestMarketTimestamp: OffsetDateTime = sourceDataConfig
            .params
            .mapNotNull { params ->
                val startDateTime = resolveStartDate(params.marketData.ticker)
                val earliestTsForTicker = processTicker(params, startDateTime)

                if (Duration.between(start, Instant.now()) > maxDuration) {
                    logger.warn(
                        "Controlled exit after ${maxDuration.toMinutes()} minutes. " +
                                "Post a request to fetch more or wait for scheduled runs to work though the load."
                    )
                    return false
                } else {
                    earliestTsForTicker
                }
            }
            .min()

        logger.info("The earliest processed marketTimestamp was $earliestMarketTimestamp")
        earliestMarketTimestamp.let {
            indexStaleRepository.suggestIndexStaleFrom(it.toLocalDate())
        }

        return true
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
        params: SourceDataParams,
        startDateTime: OffsetDateTime,
        pageToken: String = "",
        accFirstEntry: OffsetDateTime? = null
    ): OffsetDateTime? {
        val uri = buildGetHistoricalBarsUri(
            baseUrl = URI.create(alpacaProps.dailyBarsUrl),
            feed = alpacaProps.dataSource,
            symbols = listOf(params.marketData.ticker),
            timeframe = alpacaProps.barDataTimeframe,
            start = startDateTime,
            pageToken = pageToken
        )
        val response = fetch { alpacaWebClient.getRequest<BarsResponse>(uri) }
        val firstEntry = handleResponse(params, response)

        val newFirstEntry = if (accFirstEntry == null || (firstEntry != null && firstEntry < accFirstEntry)) {
            firstEntry
        } else {
            accFirstEntry
        }

        return if (response?.nextPageToken?.isNotEmpty() == true) {
            processTicker(params, startDateTime, response.nextPageToken!!, newFirstEntry)
        } else {
            newFirstEntry
        }
    }

    private fun handleResponse(params: SourceDataParams, response: BarsResponse?): OffsetDateTime? =
        response?.let { body ->
            val bars = body.bars
            val n = bars.values.sumOf { it.size }

            when {
                n == 0 -> {
                    logger.warn("Response from Alpaca is empty. Perhaps ticker: " +
                            "${params.marketData.ticker} is invalid or outdated.")
                    null
                }
                else -> {
                    logger.info("Fetched $n bars for ${params.marketData.ticker} " +
                            "up to ${bars.values.flatten().last().marketTimestamp}")
                    processEntries(params, bars.values.flatten())
                }
            }
        }

    private fun processEntries(params: SourceDataParams, barEntries: List<BarDataDto>): OffsetDateTime {
        val bars = barEntries
            .mapNotNull { entry ->
                runCatching {
                    entry.toModel(Measurement.SECURITIES_SEMI_HOURLY_BARS_RAW, params)
                }.onFailure {
                    logger.warn("Validation failed for ${params.marketData.ticker} bar data: ${it.message}")
                }.getOrNull()
            }

        marketDataFacade.save(bars)

        return bars
            .minOf { it.marketTimestamp }
            .toOffsetDateTime()
    }
}
