package net.anzop.retro.repository.influxdb

import java.time.Instant
import net.anzop.retro.model.marketData.BarData
import net.anzop.retro.model.marketData.MarketData
import net.anzop.retro.model.marketData.Measurement
import net.anzop.retro.model.marketData.PriceChange

interface MarketDataFactory<T : MarketData> {
    fun create(ticker: String, fields: Map<String, List<Any?>>): T
}

val factories: Map<String, MarketDataFactory<out MarketData>> = mapOf(
    BarData::class.java.simpleName to BarDataFactory(),
    PriceChange::class.java.simpleName to PriceChangeFactory()
)

class BarDataFactory : MarketDataFactory<BarData> {
    override fun create(ticker: String, fields: Map<String, List<Any?>>): BarData {
        val measurement = getMeasurement(ticker, fields)
        val marketTimestamp = getTimeStamp(ticker, fields)

        val openingPrice = fields["openingPrice"]
            ?.firstOrNull() as? Double
            ?: throw IllegalArgumentException("Invalid openingPrice for ticker: $ticker")

        val closingPrice = fields["closingPrice"]
            ?.lastOrNull() as? Double
            ?: throw IllegalArgumentException("Invalid closingPrice for ticker: $ticker")

        val highPrice = fields["highPrice"]
            ?.maxOfOrNull { it as Double }
            ?: throw IllegalArgumentException("Invalid highPrice for ticker: $ticker")

        val lowPrice = fields["lowPrice"]
            ?.minOfOrNull { it as Double }
            ?: throw IllegalArgumentException("Invalid lowPrice for ticker: $ticker")

        val volumeWeightedAvgPrice = fields["volumeWeightedAvgPrice"]
            ?.map { it as Double }
            ?.average()
            ?: throw IllegalArgumentException("Invalid volumeWeightedAvgPrice for ticker: $ticker")

        val totalTradingValue = fields["totalTradingValue"]
            ?.sumOf { it as Double }
            ?: throw IllegalArgumentException("Invalid totalTradingValue for ticker: $ticker")

        return BarData(
            measurement = Measurement.fromCode(measurement),
            ticker = ticker,
            marketTimestamp = marketTimestamp,
            openingPrice = openingPrice,
            closingPrice = closingPrice,
            highPrice = highPrice,
            lowPrice = lowPrice,
            volumeWeightedAvgPrice = volumeWeightedAvgPrice,
            totalTradingValue = totalTradingValue
        )
    }
}

class PriceChangeFactory : MarketDataFactory<PriceChange> {
    override fun create(ticker: String, fields: Map<String, List<Any?>>): PriceChange {
        val measurement = getMeasurement(ticker, fields)
        val marketTimestamp = getTimeStamp(ticker, fields)

        val priceChangeOpen = fields["priceChangeOpen"]
            ?.firstOrNull() as? Double
            ?: throw IllegalArgumentException("Invalid priceChangeOpen for ticker: $ticker")

        val priceChangeClose = fields["priceChangeClose"]
            ?.lastOrNull() as? Double
            ?: throw IllegalArgumentException("Invalid priceChangeClose for ticker: $ticker")

        val priceChangeHigh = fields["priceChangeHigh"]
            ?.maxOfOrNull { it as Double }
            ?: throw IllegalArgumentException("Invalid priceChangeHigh for ticker: $ticker")

        val priceChangeLow = fields["priceChangeLow"]
            ?.minOfOrNull { it as Double }
            ?: throw IllegalArgumentException("Invalid priceChangeLow for ticker: $ticker")

        val priceChangeAvg = fields["priceChangeAvg"]
            ?.map { it as Double }
            ?.average()
            ?: throw IllegalArgumentException("Invalid priceChangeAvg for ticker: $ticker")

        val priceChangeDaily = fields["priceChangeDaily"]
            ?.map { it as Double }
            ?.average()
            ?: throw IllegalArgumentException("Invalid priceChangeDaily for ticker: $ticker")

        val totalTradingValue = fields["totalTradingValue"]
            ?.sumOf { it as Double }
            ?: throw IllegalArgumentException("Invalid totalTradingValue for ticker: $ticker")

        return PriceChange(
            measurement = Measurement.fromCode(measurement),
            ticker = ticker,
            marketTimestamp = marketTimestamp,
            priceChangeOpen = priceChangeOpen,
            priceChangeClose = priceChangeClose,
            priceChangeHigh = priceChangeHigh,
            priceChangeLow = priceChangeLow,
            priceChangeAvg = priceChangeAvg,
            priceChangeDaily = priceChangeDaily,
            totalTradingValue = totalTradingValue
        )
    }
}

private fun getMeasurement(ticker: String, fields: Map<String, List<Any?>>) =
    fields["measurement"]
        ?.firstOrNull() as? String
        ?: throw IllegalArgumentException("Invalid measurement for ticker: $ticker")

private fun getTimeStamp(ticker: String, fields: Map<String, List<Any?>>) =
    fields["time"]
        ?.filterIsInstance<Instant>()
        ?.minOfOrNull { it }
        ?: throw IllegalArgumentException("Invalid marketTimestamp for ticker: $ticker")
