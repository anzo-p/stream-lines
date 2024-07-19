package net.anzop.retro.model.marketData

import java.time.Instant

sealed class MarketData(
    open val measurement: Measurement,
    open val ticker: String,
    open val marketTimestamp: Instant
)
