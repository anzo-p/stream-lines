package net.anzop.gather.http.server

import java.time.LocalDate
import net.anzop.gather.config.SourceDataConfig
import net.anzop.gather.helpers.date.toInstant
import net.anzop.gather.repository.influxdb.MarketDataFacade
import net.anzop.gather.runner.AppRunner
import net.anzop.gather.runner.FetchAndProcessAll
import net.anzop.gather.runner.FetchFinancials
import net.anzop.gather.runner.RedoIndex
import net.anzop.gather.runner.RunnerCallResult
import net.anzop.gather.service.IndexProcessor
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/maintenance")
class TaskController(
    private val appRunner: AppRunner,
    private val marketDataFacade: MarketDataFacade,
) {
    private val logger = LoggerFactory.getLogger(IndexProcessor::class.java)

    @PostMapping("/market-data/fetch")
    fun createResponse(): ResponseEntity<Map<String, String>> =
        createResponse(appRunner.fetchAndProcess(FetchAndProcessAll))

    @PostMapping("/market-data/redo-index")
    fun recalculateIndex(): ResponseEntity<Map<String, String>> =
        createResponse(appRunner.fetchAndProcess(RedoIndex))

    @DeleteMapping("/market-data/")
    fun deleteMarketDataByTickerAndDate(
        @RequestParam ticker: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) since: LocalDate
    ): ResponseEntity<String> =
        try {
            marketDataFacade.deleteBarData(ticker, since.toInstant())
            ResponseEntity.noContent().build()
        } catch (e: Exception) {
            val errorMsg = "Failed to delete data for $ticker since $since"
            logger.warn(errorMsg, e)
            ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(errorMsg)
        }

    @PostMapping("/financials/{ticker}/fetch")
    fun fetchFinancials(@PathVariable ticker: String): ResponseEntity<Map<String, String>> =
        SourceDataConfig
            .resolve(ticker)
            ?.let { createResponse(appRunner.fetchAndProcess(FetchFinancials(ticker))) }
            ?: createResponse(RunnerCallResult.TICKER_NOT_FOUND)

    private fun createResponse(result: RunnerCallResult): ResponseEntity<Map<String, String>> =
        when (result) {
            RunnerCallResult.ALREADY_RUNNING, RunnerCallResult.LOCK_UNAVAILABLE ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(mapOf("error" to result.message))

            RunnerCallResult.SUCCESS ->
                ResponseEntity.noContent().build()

            RunnerCallResult.TICKER_NOT_FOUND ->
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to result.message))
        }
}
