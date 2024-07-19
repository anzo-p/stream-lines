package net.anzop.retro.repository

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import java.time.Instant
import net.anzop.retro.model.marketData.BarData
import net.anzop.retro.model.marketData.Measurement
import net.anzop.retro.model.marketData.PriceChange

fun <T> toPoint(entity: T): Point =
    when (entity) {
        is BarData -> toPoint(entity as BarData)
        is PriceChange -> toPoint(entity as PriceChange)
        else -> throw IllegalArgumentException("Unsupported type: $entity")
    }

private fun toPoint(barData: BarData): Point =
    basePoint<BarData>(barData.measurement, barData.ticker, barData.marketTimestamp)
        .addField("openingPrice", barData.openingPrice)
        .addField("closingPrice", barData.closingPrice)
        .addField("highPrice", barData.highPrice)
        .addField("lowPrice", barData.lowPrice)
        .addField("volumeWeightedAvgPrice", barData.volumeWeightedAvgPrice)
        .addField("totalTradingValue", barData.totalTradingValue)

private fun toPoint(priceChange: PriceChange): Point =
    basePoint<PriceChange>(priceChange.measurement, priceChange.ticker, priceChange.marketTimestamp)
        .addField("priceChangeOpen", priceChange.priceChangeOpen)
        .addField("priceChangeClose", priceChange.priceChangeClose)
        .addField("priceChangeHigh", priceChange.priceChangeHigh)
        .addField("priceChangeLow", priceChange.priceChangeLow)
        .addField("priceChangeAvg", priceChange.priceChangeAvg)
        .addField("priceChangeDaily", priceChange.priceChangeDaily)
        .addField("totalTradingValue", priceChange.totalTradingValue)

private inline fun <reified T> basePoint(
    measurement: Measurement,
    ticker: String,
    marketTimestamp: Instant,
): Point =
    Point
        .measurement(measurement.code)
        .time(marketTimestamp.toEpochMilli(), WritePrecision.MS)
        .addTag("ticker", ticker)
        .addTag("type", T::class.simpleName)
