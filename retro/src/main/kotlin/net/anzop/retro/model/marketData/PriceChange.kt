package net.anzop.retro.model.marketData

import java.time.Instant
import kotlin.math.exp
import kotlin.math.ln

data class PriceChange(
    override val measurement: Measurement,
    override val company: String,
    override val ticker: String,
    override val marketTimestamp: Instant,
    override val regularTradingHours: Boolean,
    val priceChangeOpen: Double,
    val priceChangeClose: Double,
    val priceChangeHigh: Double,
    val priceChangeLow: Double,
    val priceChangeAvg: Double,
    val prevPriceChangeAvg: Double,
    val totalTradingValue: Double
) : MarketData(measurement, company, ticker, marketTimestamp, regularTradingHours)

operator fun PriceChange.plus(that: PriceChange): PriceChange =
    this.copy(
        priceChangeOpen = this.priceChangeOpen + that.priceChangeOpen,
        priceChangeClose = this.priceChangeClose + that.priceChangeClose,
        priceChangeHigh = this.priceChangeHigh + that.priceChangeHigh,
        priceChangeLow = this.priceChangeLow + that.priceChangeLow,
        priceChangeAvg = this.priceChangeAvg + that.priceChangeAvg,
        prevPriceChangeAvg = this.prevPriceChangeAvg + that.prevPriceChangeAvg,
        totalTradingValue = this.totalTradingValue + that.totalTradingValue,
    )

operator fun PriceChange.div(divisor: Double): PriceChange =
    this.copy(
        priceChangeOpen = this.priceChangeOpen / divisor,
        priceChangeClose = this.priceChangeClose / divisor,
        priceChangeHigh = this.priceChangeHigh / divisor,
        priceChangeLow = this.priceChangeLow / divisor,
        priceChangeAvg = this.priceChangeAvg / divisor,
        prevPriceChangeAvg = this.prevPriceChangeAvg / divisor,
        totalTradingValue = this.totalTradingValue
    )

fun PriceChange.ln(): PriceChange =
    this.copy(
        priceChangeOpen = ln(priceChangeOpen),
        priceChangeClose = ln(priceChangeClose),
        priceChangeHigh = ln(priceChangeHigh),
        priceChangeLow = ln(priceChangeLow),
        priceChangeAvg = ln(priceChangeAvg),
        prevPriceChangeAvg = ln(prevPriceChangeAvg),
        totalTradingValue = totalTradingValue
)

fun PriceChange.exp(): PriceChange =
    this.copy(
        priceChangeOpen = exp(priceChangeOpen),
        priceChangeClose = exp(priceChangeClose),
        priceChangeHigh = exp(priceChangeHigh),
        priceChangeLow = exp(priceChangeLow),
        priceChangeAvg = exp(priceChangeAvg),
        prevPriceChangeAvg = exp(prevPriceChangeAvg),
        totalTradingValue = totalTradingValue
)

fun Collection<PriceChange>.mean(): PriceChange =
    this.reduce { acc, priceChange -> acc + priceChange }
        .div(this.size.toDouble())

fun Collection<PriceChange>.geometricMean(): PriceChange =
    this.map { it.ln() }
        .reduce { acc, priceChange -> acc + priceChange }
        .div(this.size.toDouble())
        .exp()
