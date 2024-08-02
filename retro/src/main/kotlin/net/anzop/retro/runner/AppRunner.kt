package net.anzop.retro.runner

import java.util.concurrent.locks.ReentrantLock
import net.anzop.retro.service.BarDataFetcher
import net.anzop.retro.service.IndexProcessor
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component

@Component
class AppRunner(
    private val barDataFetcher: BarDataFetcher,
    private val indexProcessor: IndexProcessor
) {
    private val logger = LoggerFactory.getLogger(AppRunner::class.java)

    private val lock = ReentrantLock()
    @Volatile private var isRunning = false

    fun fetchAndProcess() {
        if (lock.tryLock()) {
            try {
                if (isRunning) {
                    logger.info("Lock acquirable but task is already running. Exiting...")
                    return
                }
                isRunning = true

                logger.info("Executing tasks...")
                barDataFetcher.run()
                indexProcessor.run()
                logger.info("Done.")
            } finally {
                isRunning = false
                lock.unlock()
            }
        } else {
            logger.info("Cannot acquire lock, assuming task already running. Exiting...")
        }
    }
}
