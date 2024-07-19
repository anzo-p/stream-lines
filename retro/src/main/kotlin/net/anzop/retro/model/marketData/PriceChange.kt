package net.anzop.retro.model.marketData

import java.time.Instant

data class PriceChange(
    override val measurement: Measurement,
    override val ticker: String,
    override val marketTimestamp: Instant,
    val priceChangeOpen: Double,
    val priceChangeClose: Double,
    val priceChangeHigh: Double,
    val priceChangeLow: Double,
    val priceChangeAvg: Double,
    val priceChangeDaily: Double,
    val totalTradingValue: Double
) : MarketData(measurement, ticker, marketTimestamp)

operator fun PriceChange.plus(that: PriceChange): PriceChange =
    this.copy(
        priceChangeOpen = this.priceChangeOpen + that.priceChangeOpen,
        priceChangeClose = this.priceChangeClose + that.priceChangeClose,
        priceChangeHigh = this.priceChangeHigh + that.priceChangeHigh,
        priceChangeLow = this.priceChangeLow + that.priceChangeLow,
        priceChangeAvg = this.priceChangeAvg + that.priceChangeAvg,
        priceChangeDaily = this.priceChangeDaily + that.priceChangeDaily,
        totalTradingValue = this.totalTradingValue + that.totalTradingValue,
    )

operator fun PriceChange.div(divisor: Double): PriceChange =
    this.copy(
        priceChangeOpen = this.priceChangeOpen / divisor,
        priceChangeClose = this.priceChangeClose / divisor,
        priceChangeHigh = this.priceChangeHigh / divisor,
        priceChangeLow = this.priceChangeLow / divisor,
        priceChangeAvg = this.priceChangeAvg / divisor,
        priceChangeDaily = this.priceChangeDaily / divisor,
        totalTradingValue = this.totalTradingValue
    )