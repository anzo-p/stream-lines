package net.anzop.gather.runner

import org.slf4j.LoggerFactory
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class Scheduler(private val appRunner: AppRunner) {
    private val logger = LoggerFactory.getLogger(Scheduler::class.java)

    @Scheduled(cron = "0 5 10-23 * * MON-FRI")
    @Scheduled(cron = "0 5 0 * * TUE-SAT")
    fun scheduledJob() {
        logger.info("Scheduled task running...")
        appRunner.processRunCommand(FetchMarketDataAndProcessIndex)
        logger.info("Scheduled task done.")
    }
}
