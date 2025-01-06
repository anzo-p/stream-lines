package net.anzop.gather.runner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import net.anzop.gather.service.BarDataFetcher
import net.anzop.gather.service.IndexProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AppRunner(
    private val barDataFetcher: BarDataFetcher,
    private val indexProcessor: IndexProcessor
) {
    private val logger = LoggerFactory.getLogger(AppRunner::class.java)

    private val mutex = Mutex()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    @Volatile private var isRunning = false

    fun fetchAndProcess(redoIndex: Boolean = false): RunnerCallResult =
        if (!mutex.tryLock()) {
            logger.info("${RunnerCallResult.LOCK_UNAVAILABLE.message} - Exiting...")
            RunnerCallResult.LOCK_UNAVAILABLE
        } else {
            try {
                if (isRunning) {
                    logger.info("${RunnerCallResult.ALREADY_RUNNING.message} - Exiting...")
                    RunnerCallResult.ALREADY_RUNNING
                }

                isRunning = true
                logger.info("Launching tasks...")
                coroutineScope.launch {
                    try {
                        if (!redoIndex) {
                            barDataFetcher.run()
                        }
                        indexProcessor.run()
                        logger.info("AppRunner.fetchAndProcess tasks completed.")
                    } finally {
                        isRunning = false
                        mutex.unlock()
                    }
                }

                RunnerCallResult.SUCCESS
            } catch (e: Exception) {
                isRunning = false
                mutex.unlock()
                throw e
            }
        }
}
