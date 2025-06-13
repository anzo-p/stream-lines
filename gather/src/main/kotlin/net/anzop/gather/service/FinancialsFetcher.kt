package net.anzop.gather.service

import java.net.URI
import net.anzop.gather.config.DataJockeyProps
import net.anzop.gather.config.SourceDataConfig
import net.anzop.gather.dto.financials.FinancialResponseDto
import net.anzop.gather.http.client.WebFluxExtensions.getRequest
import net.anzop.gather.http.client.buildGetFinancialsUri
import net.anzop.gather.model.SourceDataParams
import net.anzop.gather.model.financials.ReportPeriodType
import net.anzop.gather.repository.dynamodb.FinancialsRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient
import kotlin.random.Random

@Service
class FinancialsFetcher(
    private val dataJockeyProps: DataJockeyProps,
    private val sourceDataConfig: SourceDataConfig,
    private val financialsRepository: FinancialsRepository,
    private val dataJockeyWebClient: WebClient,
) : ThrottlingFetcher(dataJockeyProps.maxCallsPerMinute) {

    private val logger = LoggerFactory.getLogger(FinancialsFetcher::class.java)

    fun run(param: SourceDataParams? = null) {
        val params = param
            ?.let { listOf(it) }
            ?: sourceDataConfig
                .params
                .filterNot { it.fundamentals?.skip == true }
                .shuffled(Random(System.nanoTime()))
                .take(dataJockeyProps.companyCountPerRun)

        params.forEach { param ->
            ReportPeriodType.entries.forEach { periodType ->
                processTicker(param, periodType)
            }
        }
    }

    private fun processTicker(
        params: SourceDataParams,
        periodType: ReportPeriodType,
    ) {
        val symbol = params.fundamentals?.ticker ?: params.marketData.ticker
        logger.info("Processing $periodType type financials for $symbol")

        val uri = buildGetFinancialsUri(
            baseUrl = URI.create(dataJockeyProps.financialsUrl),
            apiKey = dataJockeyProps.authentication.apiKey,
            symbol = symbol,
            period = periodType,
        )

        fetch { dataJockeyWebClient.getRequest<FinancialResponseDto>(uri) }
            ?.let { response ->
                response
                    .toModel(periodType)
                    .filterNot { it.reportPeriod in financialsRepository.queryReportPeriods(symbol) }
                    .takeIf { it.isNotEmpty() }
                    ?.also { financials ->
                        val newReportPeriods = financials.map { it.reportPeriod.toString() }
                        logger.info("Inserting new report periods $newReportPeriods for $symbol")
                    }
                    ?.let { financialsRepository.storeFinancials(symbol, it) }
            }
    }
}
