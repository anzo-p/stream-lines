package net.anzop.gather.runner

import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Component

@Component
class AppStartupListener(private val appRunner: AppRunner) {
    private val logger = LoggerFactory.getLogger(AppStartupListener::class.java)

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        logger.info("Application started. Running initial task...")
        appRunner.fetchAndProcess()
        logger.info("Done.")
    }
}
