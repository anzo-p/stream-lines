package net.anzop.retro.model.marketData

import java.time.Instant

data class BarData(
    override val measurement: Measurement,
    override val ticker: String,
    override val marketTimestamp: Instant,
    val openingPrice: Double,
    val closingPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val volumeWeightedAvgPrice: Double,
    val totalTradingValue: Double
) : MarketData(measurement, ticker, marketTimestamp)
