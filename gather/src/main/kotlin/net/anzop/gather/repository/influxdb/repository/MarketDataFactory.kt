package net.anzop.gather.repository.influxdb.repository

import java.time.Instant
import net.anzop.gather.model.marketData.BarData
import net.anzop.gather.model.marketData.MarketData
import net.anzop.gather.model.marketData.Measurement
import net.anzop.gather.model.marketData.PriceChange

typealias InfluxDValues = Map<String, List<Any?>>

interface MarketDataFactory<T : MarketData> {
    fun create(ticker: String, values: InfluxDValues): T
}

val factories: Map<String, MarketDataFactory<out MarketData>> = mapOf(
    BarData::class.java.simpleName to BarDataFactory(),
    PriceChange::class.java.simpleName to PriceChangeFactory()
)

class BarDataFactory : MarketDataFactory<BarData> {
    override fun create(ticker: String, values: InfluxDValues): BarData {
        val openingPrice = values["openingPrice"]
            ?.firstOrNull() as? Double
            ?: throw IllegalArgumentException("Invalid openingPrice for ticker: $ticker")

        val closingPrice = values["closingPrice"]
            ?.lastOrNull() as? Double
            ?: throw IllegalArgumentException("Invalid closingPrice for ticker: $ticker")

        val highPrice = values["highPrice"]
            ?.maxOfOrNull { it as Double }
            ?: throw IllegalArgumentException("Invalid highPrice for ticker: $ticker")

        val lowPrice = values["lowPrice"]
            ?.minOfOrNull { it as Double }
            ?: throw IllegalArgumentException("Invalid lowPrice for ticker: $ticker")

        val volumeWeightedAvgPrice = values["volumeWeightedAvgPrice"]
            ?.map { it as Double }
            ?.average()
            ?: throw IllegalArgumentException("Invalid volumeWeightedAvgPrice for ticker: $ticker")

        val totalTradingValue = values["totalTradingValue"]
            ?.sumOf { it as Double }
            ?: throw IllegalArgumentException("Invalid totalTradingValue for ticker: $ticker")

        return BarData(
            measurement = Measurement.fromCode(getMeasurement(ticker, values)),
            company = getCompany(ticker, values),
            ticker = ticker,
            marketTimestamp = getTimeStamp(ticker, values),
            regularTradingHours = getRegularTradingHours(ticker, values),
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
    override fun create(ticker: String, values: Map<String, List<Any?>>): PriceChange {
        val priceChangeOpen = values["priceChangeOpen"]
            ?.firstOrNull() as? Double
            ?: throw IllegalArgumentException("Invalid priceChangeOpen for ticker: $ticker")

        val priceChangeClose = values["priceChangeClose"]
            ?.lastOrNull() as? Double
            ?: throw IllegalArgumentException("Invalid priceChangeClose for ticker: $ticker")

        val priceChangeHigh = values["priceChangeHigh"]
            ?.maxOfOrNull { it as Double }
            ?: throw IllegalArgumentException("Invalid priceChangeHigh for ticker: $ticker")

        val priceChangeLow = values["priceChangeLow"]
            ?.minOfOrNull { it as Double }
            ?: throw IllegalArgumentException("Invalid priceChangeLow for ticker: $ticker")

        val priceChangeAvg = values["priceChangeAvg"]
            ?.map { it as Double }
            ?.average()
            ?: throw IllegalArgumentException("Invalid priceChangeAvg for ticker: $ticker")

        val prevPriceChangeAvg = values["prevPriceChangeAvg"]
            ?.map { it as Double }
            ?.average()
            ?: throw IllegalArgumentException("Invalid prevPriceChangeAvg for ticker: $ticker")

        val totalTradingValue = values["totalTradingValue"]
            ?.sumOf { it as Double }
            ?: throw IllegalArgumentException("Invalid totalTradingValue for ticker: $ticker")

        return PriceChange(
            measurement = Measurement.fromCode(getMeasurement(ticker, values)),
            company = getCompany(ticker, values),
            ticker = ticker,
            marketTimestamp = getTimeStamp(ticker, values),
            regularTradingHours = getRegularTradingHours(ticker, values),
            priceChangeOpen = priceChangeOpen,
            priceChangeClose = priceChangeClose,
            priceChangeHigh = priceChangeHigh,
            priceChangeLow = priceChangeLow,
            priceChangeAvg = priceChangeAvg,
            prevPriceChangeAvg = prevPriceChangeAvg,
            totalTradingValue = totalTradingValue
        )
    }
}

private fun getMeasurement(ticker: String, values: InfluxDValues): String =
    values["measurement"]
        ?.firstOrNull() as? String
        ?: throw IllegalArgumentException("Invalid measurement for ticker: $ticker")

private fun getTimeStamp(ticker: String, values: InfluxDValues): Instant =
    values["time"]
        ?.filterIsInstance<Instant>()
        ?.minOfOrNull { it }
        ?: throw IllegalArgumentException("Invalid marketTimestamp for ticker: $ticker")

private fun getCompany(ticker: String, values: InfluxDValues): String =
    values["company"]
        ?.firstOrNull() as? String
        ?: throw IllegalArgumentException("Invalid company for ticker: $ticker")

private fun getRegularTradingHours(ticker: String, values: InfluxDValues): Boolean {
    val value = values["regularTradingHours"]
        // first..() works as presently regularTradingHours is a query param
        ?.firstOrNull() as? String
        ?: throw IllegalArgumentException("Invalid regularTradingHours for ticker: $ticker")

    return value.toBooleanStrictOrNull()
        ?: throw IllegalArgumentException("Invalid regularTradingHours $value for ticker: $ticker")
}
