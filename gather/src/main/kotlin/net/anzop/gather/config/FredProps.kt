package net.anzop.gather.config

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import java.time.LocalDate
import net.anzop.gather.http.client.WebClientFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import org.springframework.web.reactive.function.client.WebClient

@Component
@ConfigurationProperties(prefix = "fred")
@Validated
class FredProps {
    class Authentication {
        var apiKey: String = ""
    }

    var authentication: Authentication = Authentication()

    @NotNull
    lateinit var earliestHistoricalDate: LocalDate

    @Min(1)
    var maxCallsPerMinute: Int = 1

    @NotBlank
    lateinit var vixUrl: String
}

@Configuration
class YahooConfig(
    private val factory: WebClientFactory,
) {

    @Bean(name = ["fredWebClient"])
    fun fredWebClient(): WebClient = factory.builder().build()
}
