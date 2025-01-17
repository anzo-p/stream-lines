package net.anzop.gather.http

import java.time.LocalDate
import net.anzop.gather.helpers.date.toInstant
import net.anzop.gather.repository.influxdb.MarketDataFacade
import net.anzop.gather.runner.AppRunner
import net.anzop.gather.runner.RunnerCallResult
import net.anzop.gather.service.IndexProcessor
import org.slf4j.LoggerFactory
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/admin/maintenance")
class TaskController(
    private val appRunner: AppRunner,
    private val marketDataFacade: MarketDataFacade
) {
    private val logger = LoggerFactory.getLogger(IndexProcessor::class.java)

    private fun fetchAndProcess(result: RunnerCallResult): ResponseEntity<Map<String, String>> =
        when (result) {
            RunnerCallResult.ALREADY_RUNNING, RunnerCallResult.LOCK_UNAVAILABLE ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(mapOf("error" to result.message))

            RunnerCallResult.SUCCESS ->
                ResponseEntity.noContent().build()
        }

    @PostMapping("/fetch")
    fun fetchAndProcess(): ResponseEntity<Map<String, String>> =
        fetchAndProcess(appRunner.fetchAndProcess(redoIndex = false))

    @PostMapping("/redo-index")
    fun recalculateIndex(): ResponseEntity<Map<String, String>> =
        fetchAndProcess(appRunner.fetchAndProcess(redoIndex = true))

    @DeleteMapping("/bar-data")
    fun deleteBarDataByTickerAndDate(
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
}
