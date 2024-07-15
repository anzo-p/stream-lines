package net.anzop.retro.model

import java.time.OffsetDateTime

data class PriceChangeWeighted(
    val measurement: Measurement,
    val ticker: String,
    val marketTimestamp: OffsetDateTime,
    val priceChangeOpen: Double,
    val priceChangeClose: Double,
    val priceChangeHigh: Double,
    val priceChangeLow: Double,
    val priceChangeAvg: Double,
    val priceChangeDaily: Double,
    val totalTradingValue: Double
)

operator fun PriceChangeWeighted.plus(that: PriceChangeWeighted): PriceChangeWeighted =
    this.copy(
        priceChangeOpen = this.priceChangeOpen + that.priceChangeOpen,
        priceChangeClose = this.priceChangeClose + that.priceChangeClose,
        priceChangeHigh = this.priceChangeHigh + that.priceChangeHigh,
        priceChangeLow = this.priceChangeLow + that.priceChangeLow,
        priceChangeAvg = this.priceChangeAvg + that.priceChangeAvg,
        priceChangeDaily = this.priceChangeDaily + that.priceChangeDaily,
        totalTradingValue = this.totalTradingValue + that.totalTradingValue,
    )

operator fun PriceChangeWeighted.div(divisor: Double): PriceChangeWeighted =
    this.copy(
        priceChangeOpen = this.priceChangeOpen / divisor,
        priceChangeClose = this.priceChangeClose / divisor,
        priceChangeHigh = this.priceChangeHigh / divisor,
        priceChangeLow = this.priceChangeLow / divisor,
        priceChangeAvg = this.priceChangeAvg / divisor,
        priceChangeDaily = this.priceChangeDaily / divisor,
        totalTradingValue = this.totalTradingValue
    )
