package net.anzop.retro.config

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
@ConfigurationProperties(prefix = "alpaca")
class AlpacaProps {
    class Authentication {
        @NotBlank
        lateinit var apiKey: String

        @NotBlank
        lateinit var apiSecret: String
    }

    lateinit var authentication: Authentication

    @NotBlank
    lateinit var dailyBarsUrl: String

    @NotBlank
    lateinit var dataSource: String

    @NotNull
    lateinit var earliestHistoricalDate: LocalDate

    @Min(1)
    var maxCallsPerMinute: Int = 1
}

@Configuration
class AlpacaConfig(private val alpacaProperties: AlpacaProps) {

    @Bean
    fun webClient(): WebClient {
        return WebClient
            .builder()
            .defaultHeaders { it.authenticate(alpacaProperties.authentication) }
            .build()
    }

    private fun HttpHeaders.authenticate(auth: AlpacaProps.Authentication) {
        set("Apca-Api-Key-Id", auth.apiKey)
        set("Apca-Api-Secret-Key", auth.apiSecret)
    }
}
