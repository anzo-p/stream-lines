package net.anzop.retro.model

import java.time.OffsetDateTime

data class BarData(
    val measurement: Measurement,
    val ticker: String,
    val marketTimestamp: OffsetDateTime,
    val openingPrice: Double,
    val closingPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val volumeWeightedAvgPrice: Double,
    val totalTradingValue: Double
)
