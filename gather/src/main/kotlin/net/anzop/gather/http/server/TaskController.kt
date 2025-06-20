package net.anzop.gather.http.server

import java.time.LocalDate
import net.anzop.gather.config.SourceDataConfig
import net.anzop.gather.helpers.date.toInstant
import net.anzop.gather.runner.AppRunner
import net.anzop.gather.runner.DeleteMarketData
import net.anzop.gather.runner.FetchFinancials
import net.anzop.gather.runner.FetchMarketDataAndProcessIndex
import net.anzop.gather.runner.RedoIndex
import net.anzop.gather.runner.RunCommandResult
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
) {
    @PostMapping("/financials/{ticker}/fetch")
    fun fetchFinancials(@PathVariable ticker: String): HttpResponse =
        handleAndRespond {
            resolveTickerAndRun(ticker) {
                appRunner.processRunCommand(FetchFinancials(ticker))
            }
        }

    @PostMapping("/market-data/fetch")
    fun fetchAll(): HttpResponse =
        handleAndRespond {
            appRunner.processRunCommand(FetchMarketDataAndProcessIndex)
        }

    @PostMapping("/market-data/redo-index")
    fun redoIndex(): HttpResponse =
        handleAndRespond {
            appRunner.processRunCommand(RedoIndex)
        }

    @DeleteMapping("/market-data")
    fun deleteBarData(
        @RequestParam ticker: String,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) since: LocalDate
    ): HttpResponse =
        handleAndRespond {
            resolveTickerAndRun(ticker) {
                appRunner.processRunCommand(DeleteMarketData(ticker, since.toInstant()))
            }
        }

    private inline fun resolveTickerAndRun(
        ticker: String,
        command: () -> RunCommandResult
    ): RunCommandResult =
        SourceDataConfig
            .resolve(ticker)
            ?.let { command() }
            ?: RunCommandResult.TICKER_NOT_FOUND

    private inline fun handleAndRespond(task: () -> RunCommandResult): HttpResponse =
        when (val result = task()) {
            RunCommandResult.SUCCESS ->
                ResponseEntity.noContent().build()

            RunCommandResult.ALREADY_RUNNING, RunCommandResult.LOCK_UNAVAILABLE ->
                ResponseEntity
                    .status(HttpStatus.CONFLICT)
                    .body(mapOf("error" to result.message))

            RunCommandResult.TICKER_NOT_FOUND ->
                ResponseEntity
                    .status(HttpStatus.NOT_FOUND)
                    .body(mapOf("error" to result.message))
        }
}
