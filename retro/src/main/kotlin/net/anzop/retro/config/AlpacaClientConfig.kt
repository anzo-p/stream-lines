package net.anzop.retro.config

import jakarta.annotation.PostConstruct
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.reactive.function.client.WebClient

@Configuration
@ConfigurationProperties(prefix = "alpaca")
class AlpacaClientConfig {

    lateinit var dailyBarsUrl: String
    lateinit var earliestHistoricalDate: LocalDate
    lateinit var earliestDate: OffsetDateTime
    var maxCallsPerMinute: Int = 0

    @Bean
    fun webClient(): WebClient {
        return WebClient.builder().baseUrl(dailyBarsUrl).build()
    }

    @PostConstruct
    private fun init() {
        earliestDate = earliestHistoricalDate.atStartOfDay().atOffset(ZoneOffset.UTC)
    }
}
