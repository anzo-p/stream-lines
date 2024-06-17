package net.anzop.retro.model

import java.time.OffsetDateTime
import java.util.*

data class BarData(
    val id: UUID = UUID.randomUUID(),
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
