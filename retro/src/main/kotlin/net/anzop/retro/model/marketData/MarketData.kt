package net.anzop.retro.model.marketData

import java.time.Instant

sealed class MarketData(
    open val measurement: Measurement,
    open val company: String,
    open val ticker: String,
    open val marketTimestamp: Instant,
    open val regularTradingHours: Boolean
)
