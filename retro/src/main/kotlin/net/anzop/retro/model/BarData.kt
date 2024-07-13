package net.anzop.retro.model

import java.time.OffsetDateTime

enum class Measurement(val code: String) {
    SECURITIES_RAW_DAILY("sec_raw_d");

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
