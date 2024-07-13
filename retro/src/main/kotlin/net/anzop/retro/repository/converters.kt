package net.anzop.retro.repository

import com.influxdb.query.FluxTable
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import net.anzop.retro.model.BarData
import net.anzop.retro.model.Measurement

fun toBarDataList(tables: List<FluxTable>): List<BarData> {
    val groupedByTicker = tables.flatMap { it.records }
        .groupBy { record -> record.getValueByKey("ticker")?.toString() ?: "" }
        .filterKeys { it.isNotEmpty() }

    return groupedByTicker.map { (ticker, records) ->
        val fields = mutableMapOf<String, MutableList<Any?>>()

        records.forEach { record ->
            val field = record.field ?: return@forEach
            val value = record.value ?: return@forEach

            fields["time"] = mutableListOf(record.time)
            fields["_measurement"] = mutableListOf(record.measurement)
            val values = fields.getOrPut(field) { mutableListOf() }
            values.add(value)
        }

        toBarData(ticker, fields)
    }
}

private fun toBarData(ticker: String, fields: Map<String, List<Any?>>): BarData {
    val measurement = (fields["_measurement"] as? List<*>)
        ?.filterIsInstance<String>()
        ?.firstOrNull()
        ?.let { Measurement.fromCode(it) }
        ?: throw IllegalArgumentException("Invalid measurement for ticker: $ticker")

    val openingPrice = (fields["openingPrice"] as? List<*>)
        ?.filterIsInstance<Number>()
        ?.firstOrNull()
        ?.toDouble()
        ?: throw IllegalArgumentException("Invalid openingPrice for ticker: $ticker")

    val closingPrice = (fields["closingPrice"] as? List<*>)
        ?.filterIsInstance<Number>()
        ?.firstOrNull()
        ?.toDouble()
        ?: throw IllegalArgumentException("Invalid closingPrice for ticker: $ticker")

    val highPrice = (fields["highPrice"] as? List<*>)
        ?.filterIsInstance<Number>()
        ?.firstOrNull()
        ?.toDouble()
        ?: throw IllegalArgumentException("Invalid highPrice for ticker: $ticker")

    val lowPrice = (fields["lowPrice"] as? List<*>)
        ?.filterIsInstance<Number>()
        ?.firstOrNull()
        ?.toDouble()
        ?: throw IllegalArgumentException("Invalid lowPrice for ticker: $ticker")

    val volumeWeightedAvgPrice = (fields["volumeWeightedAvgPrice"] as? List<*>)
        ?.filterIsInstance<Number>()
        ?.firstOrNull()
        ?.toDouble()
        ?: throw IllegalArgumentException("Invalid volumeWeightedAvgPrice for ticker: $ticker")

    val totalTradingValue = (fields["totalTradingValue"] as? List<*>)
        ?.filterIsInstance<Number>()
        ?.firstOrNull()
        ?.toDouble()
        ?: throw IllegalArgumentException("Invalid totalTradingValue for ticker: $ticker")

    val marketTimestamp = (fields["time"] as? List<*>)
        ?.filterIsInstance<Instant>()
        ?.firstOrNull()
        ?.let { OffsetDateTime.ofInstant(it, ZoneOffset.UTC) }
        ?: throw IllegalArgumentException("Invalid marketTimestamp for ticker: $ticker")

    return BarData(
        measurement = measurement,
        ticker = ticker,
        openingPrice = openingPrice,
        closingPrice = closingPrice,
        highPrice = highPrice,
        lowPrice = lowPrice,
        volumeWeightedAvgPrice = volumeWeightedAvgPrice,
        totalTradingValue = totalTradingValue,
        marketTimestamp = marketTimestamp
    )
}
