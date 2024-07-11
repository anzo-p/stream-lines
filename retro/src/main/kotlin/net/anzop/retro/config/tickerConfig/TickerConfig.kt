package net.anzop.retro.config.tickerConfig

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

data class Ticker(

    @NotBlank
    @field:Pattern(regexp = "^[A-Z.]{1,5}\$", message = "Symbol must have 1-5 uppercase letters. Period also allowed.")
    val symbol: String,

    @NotBlank
    val company: String,

    val series: String?
)

@Component
@ConfigurationProperties
@Validated
@UniqueSymbols
@UniqueCompany
data class TickerConfig(
    val tickers: List<Ticker>
)
