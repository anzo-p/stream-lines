package net.anzop.gather.service

import java.net.URI
import java.time.LocalDate
import net.anzop.gather.config.FredProps
import net.anzop.gather.dto.economics.VixResponse
import net.anzop.gather.helpers.date.toOffsetDateTime
import net.anzop.gather.http.client.WebFluxExtensions.getRequest
import net.anzop.gather.http.client.buildGetVixUri
import net.anzop.gather.repository.influxdb.MarketDataFacade
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class VixFetcher(
    private val fredProps: FredProps,
    private val marketDataFacade: MarketDataFacade,
    private val fredWebClient: WebClient
): ThrottlingFetcher(fredProps.maxCallsPerMinute) {

    private val logger = LoggerFactory.getLogger(VixFetcher::class.java)

    fun run() {
        val startDate = resolveStartDate()
        process(startDate)
    }

    private fun resolveStartDate(): LocalDate =
        marketDataFacade
            .getLatestVixEntry()
            .also { logger.info("Last known entry for Vix is $it") }
            ?.toOffsetDateTime()
            ?.toLocalDate()
            ?: fredProps
                .earliestHistoricalDate
                .also { logger.info("No db entry for Vix, defaulting to: $it") }

    private fun process(start: LocalDate) =
        fetch(start)?.let { handleResponse(it) }

    private fun fetch(
        start: LocalDate,
    ): VixResponse? {
        val uri = buildGetVixUri(
            baseUrl = URI.create(fredProps.vixUrl),
            apiKey = fredProps.authentication.apiKey,
            startDate = start
        )
        return fetch { fredWebClient.getRequest<VixResponse>(uri) }
    }

    private fun handleResponse(response: VixResponse) {
        response.observations.forEach { observation ->
            observation
                .toModel()
                ?.let { marketDataFacade.save(it) }
                ?: logger.warn(
                    "Skipping Vix observation with invalid value: ${observation.value} for date ${observation.date}")
        }
    }
}
