package net.anzop.gather.runner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withTimeoutOrNull
import net.anzop.gather.config.SourceDataConfig
import net.anzop.gather.service.BarDataFetcher
import net.anzop.gather.service.FinancialsFetcher
import net.anzop.gather.service.IndexProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import kotlin.time.Duration.Companion.minutes

@Component
class AppRunner(
    private val barDataFetcher: BarDataFetcher,
    private val indexProcessor: IndexProcessor,
    private val financialsFetcher: FinancialsFetcher,
) {
    private val logger = LoggerFactory.getLogger(AppRunner::class.java)

    private val mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    @Volatile private var isRunning = false

    fun fetchAndProcess(command: RunCommand): RunnerCallResult {
        if (!mutex.tryLock()) {
            logger.info("${RunnerCallResult.LOCK_UNAVAILABLE.message} - Exiting...")
            return RunnerCallResult.LOCK_UNAVAILABLE
        }

        if (isRunning) {
            logger.info("${RunnerCallResult.ALREADY_RUNNING.message} - Exiting...")
            mutex.unlock()
            return RunnerCallResult.ALREADY_RUNNING
        }

        isRunning = true
        logger.info("Launching tasks...")

        coroutineScope.launch {
            withTimeoutOrNull(15.minutes) {
                try {
                    when (command) {
                        is FetchAndProcessAll -> {
                            barDataFetcher.run()
                            indexProcessor.run()
                            financialsFetcher.run()
                        }

                        is FetchFinancials ->
                            SourceDataConfig
                                .resolve(command.ticker)
                                ?.let { financialsFetcher.run(it) }

                        is RedoIndex ->
                            indexProcessor.run()
                    }
                    logger.info("AppRunner.fetchAndProcess coroutine tasks completed.")
                } catch (e: Exception) {
                    logger.error("AppRunner.fetchAndProcess coroutine failed.", e)
                } finally {
                    isRunning = false
                    mutex.unlock()
                }
            }
        }

        return RunnerCallResult.SUCCESS
    }
}
