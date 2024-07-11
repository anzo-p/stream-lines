package net.anzop.retro

import net.anzop.retro.service.BarDataFetcher
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service

@Service
class ApplicationStartupListener(
    private val barDataFetcher: BarDataFetcher
) {
    private val logger = LoggerFactory.getLogger(ApplicationStartupListener::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        logger.info("Application started. Running initial task...")
        barDataFetcher.run()
    }
}