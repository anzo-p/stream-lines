package net.anzop.gather.runner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import net.anzop.gather.config.AlpacaProps
import net.anzop.gather.config.SourceDataConfig
import net.anzop.gather.service.BarDataFetcher
import net.anzop.gather.service.FinancialsFetcher
import net.anzop.gather.service.IndexProcessor
import net.anzop.gather.service.VixFetcher
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.time.Duration.Companion.minutes

@Component
class AppRunner(
    private val alpacaProps: AlpacaProps,
    private val barDataFetcher: BarDataFetcher,
    private val financialsFetcher: FinancialsFetcher,
    private val indexProcessor: IndexProcessor,
    private val vixFetcher: VixFetcher,
) {
    private val logger = LoggerFactory.getLogger(AppRunner::class.java)

    private val mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    @Volatile private var isRunning = false

    fun processRunCommand(command: RunCommand): RunCommandResult {
        logger.info("AppRunner.processRunCommand: $command")

        if (!mutex.tryLock()) {
            logger.info("${RunCommandResult.LOCK_UNAVAILABLE.message} - Exiting...")
            return RunCommandResult.LOCK_UNAVAILABLE
        }

        if (isRunning) {
            logger.info("${RunCommandResult.ALREADY_RUNNING.message} - Exiting...")
            mutex.unlock()
            return RunCommandResult.ALREADY_RUNNING
        }

        isRunning = true

        coroutineScope.launch {
            try {
                val timeoutMinutes = alpacaProps.maxExecDurationMinutes.minutes.plus(1.minutes)
                withTimeoutOrNull(timeoutMinutes) {
                    when (command) {
                        is FetchFinancials ->
                            SourceDataConfig
                                .resolve(command.ticker)
                                ?.let { financialsFetcher.run(it) }

                        is FetchMarketDataAndProcessIndex -> {
                            if (barDataFetcher.run()) {
                                indexProcessor.run()
                                //financialsFetcher.run()
                                vixFetcher.run()
                            } else {
                                logger.warn("Skipping index processing and fetch for financials.")
                            }
                        }

                        is RedoIndex ->
                            indexProcessor.run()
                    }
                    logger.info("AppRunner.fetchAndProcess coroutine tasks completed.")
                } ?: logger.warn("AppRunner.fetchAndProcess timed out after $timeoutMinutes minutes.")
            } catch (e: Exception) {
                logger.error("AppRunner.fetchAndProcess coroutine failed.", e)
            } finally {
                isRunning = false
                mutex.unlock()
            }
        }

        return RunCommandResult.DISPATCHED
    }
}
