package net.anzop.retro.repository.influxdb

import com.influxdb.query.FluxRecord
import com.influxdb.query.FluxTable
import java.time.Instant
import net.anzop.retro.model.marketData.MarketData

fun <T : MarketData> parseTable(tables: List<FluxTable>, factory: MarketDataFactory<T>): List<T> =
    toRecords(tables).mapNotNull { (ticker, records) ->
        val values = mutableMapOf<String, MutableList<Any?>>()

        fun addOnce(key: String, value: Any) =
            (key !in values).let { values[key] = mutableListOf(value) }

        fun add(key: String, value: Any) =
            values.getOrPut(key) { mutableListOf() }.add(value)

        records
            .sortedBy { record -> record.getValueByKey("_time") as? Instant }
            .forEach { record ->
                val recordField = record.field ?: return@forEach
                record.values.forEach { (key, value) ->
                    when (key) {
                        "_time" -> addOnce("time", value)
                        "_measurement" -> addOnce("measurement", value)
                        "_value" -> add(recordField, value)
                    }
                }
            }

        factory.create(ticker, values)
    }

private fun toRecords(tables: List<FluxTable>): Map<String, List<FluxRecord>> =
    tables
        .flatMap { it.records }
        .mapNotNull { record ->
            record
                .getValueByKey("ticker")
                ?.toString()
                ?.let { it to record }
        }
        .groupBy({ it.first }, { it.second })
