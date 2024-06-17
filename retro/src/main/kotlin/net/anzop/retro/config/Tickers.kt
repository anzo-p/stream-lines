package net.anzop.retro.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

@ConfigurationProperties
@Component
data class Tickers(
    val tickers: List<String>
)
