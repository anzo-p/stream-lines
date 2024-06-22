package net.anzop.retro.model

import java.time.OffsetDateTime

data class BarData(
    val ticker: String,
    val barTimeSpan: String,
    val openingPrice: Double,
    val closingPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val numberOfTrades: Long,
    val volume: Long,
    val volumeWeighted: Double,
    val marketTimestamp: OffsetDateTime,
)
