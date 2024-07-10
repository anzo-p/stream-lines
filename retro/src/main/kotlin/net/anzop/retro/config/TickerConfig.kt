package net.anzop.retro.config

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component

data class Ticker(
    val symbol: String,
    val company: String,
    val series: String?
)

@ConfigurationProperties
@Component
data class TickerConfig(
    val tickers: List<Ticker>
)
