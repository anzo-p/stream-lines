package net.anzop.gather.config

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotBlank
import net.anzop.gather.http.client.WebClientFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated
import org.springframework.web.reactive.function.client.WebClient

@Component
@ConfigurationProperties(prefix = "datajockey")
@Validated
class DataJockeyProps {
    class Authentication {
        @NotBlank
        lateinit var apiKey: String
    }

    lateinit var authentication: Authentication

    @NotBlank
    lateinit var financialsUrl: String

    @Min(1)
    var maxCallsPerMinute: Int = 1

    @Min(0)
    var companyCountPerRun: Int = 0
}

@Configuration
class DataJockeyConfig(
    private val factory: WebClientFactory
) {
    @Bean(name = ["dataJockeyWebClient"])
    fun dataJockeyWebClient(): WebClient = factory.builder().build()
}
