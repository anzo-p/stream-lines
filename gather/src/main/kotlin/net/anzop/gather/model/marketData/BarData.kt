package net.anzop.gather.model.marketData

import java.time.Instant

data class BarData(
    override val measurement: Measurement,
    override val company: String,
    override val ticker: String,
    override val marketTimestamp: Instant,
    override val regularTradingHours: Boolean,
    val openingPrice: Double,
    val closingPrice: Double,
    val highPrice: Double,
    val lowPrice: Double,
    val volumeWeightedAvgPrice: Double,
    val totalTradingValue: Double
) : MarketData(measurement, company, ticker, marketTimestamp, regularTradingHours)
