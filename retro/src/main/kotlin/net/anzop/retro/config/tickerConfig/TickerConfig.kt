package net.anzop.retro.config.tickerConfig

import net.anzop.retro.model.Ticker
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@ConfigurationProperties
@Validated
@UniqueSymbols
@UniqueCompany
data class TickerConfig(
    val tickers: List<Ticker>
)
