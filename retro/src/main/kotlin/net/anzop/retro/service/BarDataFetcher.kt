package net.anzop.retro.service

import net.anzop.retro.config.AlpacaClientConfig
import net.anzop.retro.config.Tickers
import net.anzop.retro.repository.BarDataRepository
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.stereotype.Service
import org.springframework.web.reactive.function.client.WebClient

@Service
class BarDataFetcher(
    private val tickerConfig: Tickers,
    private val alpacaClientConfig: AlpacaClientConfig,
    private val webClient: WebClient,
    private val barDataRepository: BarDataRepository
) {

    @EventListener(ApplicationReadyEvent::class)
    fun onApplicationReady() {
        println("Application started. Running initial task...")
        run()
    }

    private fun run() {
        tickerConfig.tickers.forEach {
            println(barDataRepository.getLatestMeasurementTime(it) ?: alpacaClientConfig.earliestDate)

            /*
                for all tickers
                    find latest
                        are there weekdays since
                        can we even filter to are there banking days in between

                            compose uri
                            call
                            handle response
                                200?
                                    log count of items
                                    handle item
                                    next_token not null?
                                         call again with that
                                other?
                                    log error
                                    break
             */

        }
    }
}
