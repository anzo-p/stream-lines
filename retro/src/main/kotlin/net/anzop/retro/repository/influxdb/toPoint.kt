package net.anzop.retro.repository.influxdb

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import net.anzop.retro.model.marketData.BarData
import net.anzop.retro.model.marketData.MarketData
import net.anzop.retro.model.marketData.PriceChange

fun <T> toPoint(entity: T): Point =
    when (entity) {
        is BarData -> (entity as BarData).toPoint()
        is PriceChange -> (entity as PriceChange).toPoint()
        else -> throw IllegalArgumentException("Unsupported type: $entity")
    }

private inline fun <reified T : MarketData> basePoint(data: MarketData): Point =
    Point
        .measurement(data.measurement.code)
        .time(data.marketTimestamp.toEpochMilli(), WritePrecision.MS)
        .addTag("ticker", data.ticker)

private fun BarData.toPoint(): Point =
    basePoint<BarData>(this)
        .addFields(
            mapOf(
                "openingPrice" to openingPrice,
                "closingPrice" to closingPrice,
                "highPrice" to highPrice,
                "lowPrice" to lowPrice,
                "volumeWeightedAvgPrice" to volumeWeightedAvgPrice,
                "totalTradingValue" to totalTradingValue,
            )
        )

private fun PriceChange.toPoint(): Point =
    basePoint<PriceChange>(this)
        .addFields(
            mapOf(
                "priceChangeOpen" to priceChangeOpen,
                "priceChangeClose" to priceChangeClose,
                "priceChangeHigh" to priceChangeHigh,
                "priceChangeLow" to priceChangeLow,
                "priceChangeAvg" to priceChangeAvg,
                "priceChangeDaily" to priceChangeDaily,
                "totalTradingValue" to totalTradingValue,
            )
        )
