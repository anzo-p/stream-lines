package net.anzop.retro.runner

import java.time.Instant
import java.time.ZoneId
import net.anzop.retro.helpers.date.isHoliday
import net.anzop.retro.helpers.date.toLocalDate
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

    fun fetchAndProcess() {
        logger.info("Executing tasks...")

        if (Instant.now()
            .toLocalDate(ZoneId.of("America/New_York"))
            .isHoliday()) {

            logger.info("Today is a holiday in U.S. and securities exchanges aren't open. Exiting...")
            return
        }

        barDataFetcher.run()
        logger.info("Bar data fetching completed.")
        indexProcessor.process()
        logger.info("Index processing completed.")
    }
}
