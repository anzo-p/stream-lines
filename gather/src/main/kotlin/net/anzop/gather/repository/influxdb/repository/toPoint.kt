package net.anzop.gather.repository.influxdb.repository

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import net.anzop.gather.model.marketData.BarData
import net.anzop.gather.model.marketData.MarketData
import net.anzop.gather.model.marketData.PriceChange

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
        .addTags(
            mapOf(
                "company" to data.company,
                "ticker" to data.ticker,
                "regularTradingHours" to data.regularTradingHours.toString(),
            )
        )

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
                "prevPriceChangeAvg" to prevPriceChangeAvg,
                "totalTradingValue" to totalTradingValue,
            )
        )
