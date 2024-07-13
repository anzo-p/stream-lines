package net.anzop.retro.model

import java.time.OffsetDateTime

enum class Measurement(val code: String) {
    SECURITIES_RAW_DAILY("sec_raw_d"),
    SECURITIES_WEIGHTED_EQUAL_DAILY("sec_w_eq_d"),
    INDEX_WEIGHTED_EQUAL_DAILY("ix_w_eq_d");

    companion object {
        private val map = entries.associateBy(Measurement::code)

        fun fromCode(code: String): Measurement {
            return map[code] ?: throw IllegalArgumentException("Invalid code: $code for enum Measurement")
        }
    }
}

data class BarData(
    val measurement: Measurement,
    val ticker: String,
    val openingPrice: Double,
    val closingPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val volumeWeightedAvgPrice: Double,
    val totalTradingValue: Double,
    val marketTimestamp: OffsetDateTime
)

operator fun BarData.plus(that: BarData): BarData =
    BarData(
        measurement = this.measurement,
        ticker = this.ticker,
        openingPrice = this.openingPrice + that.openingPrice,
        closingPrice = this.closingPrice + that.closingPrice,
        highPrice = this.highPrice + that.highPrice,
        lowPrice = this.lowPrice + that.lowPrice,
        volumeWeightedAvgPrice = this.volumeWeightedAvgPrice + that.volumeWeightedAvgPrice,
        totalTradingValue = this.totalTradingValue + that.totalTradingValue,
        marketTimestamp = that.marketTimestamp,
    )

operator fun BarData.div(divisor: Double): BarData =
    this.copy(
        openingPrice = this.openingPrice / divisor,
        closingPrice = this.closingPrice / divisor,
        highPrice = this.highPrice / divisor,
        lowPrice = this.lowPrice / divisor,
        volumeWeightedAvgPrice = this.volumeWeightedAvgPrice / divisor,
        totalTradingValue = this.totalTradingValue
    )
