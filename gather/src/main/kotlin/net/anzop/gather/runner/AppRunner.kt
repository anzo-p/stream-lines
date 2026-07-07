package net.anzop.gather.runner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
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

    fun runCommandBlocking(command: RunCommand): RunCommandResult {
        logger.info("AppRunner.processRunCommandBlocking: $command")

        return withRunLockBlocking {
            runBlocking {
                executeCommand(command)
            }
        }
    }

    fun dispatchRunCommand(command: RunCommand): RunCommandResult {
        logger.info("AppRunner.processRunCommand: $command")

        return withRunLockAsync {
            executeCommand(command)
        }
    }

    private fun withRunLockBlocking(block: () -> Unit): RunCommandResult {
        if (!mutex.tryLock()) {
            logger.info("${RunCommandResult.LOCK_UNAVAILABLE.message} - Exiting...")
            return RunCommandResult.LOCK_UNAVAILABLE
        }

        return try {
            block()
            RunCommandResult.DISPATCHED
        } finally {
            mutex.unlock()
        }
    }

    private fun withRunLockAsync(block: suspend () -> Unit): RunCommandResult {
        if (!mutex.tryLock()) {
            logger.info("${RunCommandResult.LOCK_UNAVAILABLE.message} - Exiting...")
            return RunCommandResult.LOCK_UNAVAILABLE
        }

        try {
            coroutineScope
                .launch {
                    try {
                        block()
                    } catch (e: Exception) {
                        logger.error("AppRunner.fetchAndProcess coroutine failed.", e)
                    }
                }
                .invokeOnCompletion {
                    mutex.unlock()
                }
        } catch (e: Exception) {
            mutex.unlock()
            throw e
        }

        return RunCommandResult.DISPATCHED
    }

    private suspend fun executeCommand(command: RunCommand) {
        val timeoutMinutes = alpacaProps.maxExecDurationMinutes.minutes.plus(1.minutes)
        withTimeout(timeoutMinutes) {
            withContext(Dispatchers.IO) {
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
                            logger.warn("BarDataFetcher exited without completion, skipping further processing.")
                        }
                    }

                    is RedoIndex ->
                        indexProcessor.run()
                }
                logger.info("AppRunner.fetchAndProcess tasks completed.")
            }
        }
    }
}
