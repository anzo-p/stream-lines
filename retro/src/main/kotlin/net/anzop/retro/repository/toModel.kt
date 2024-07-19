package net.anzop.retro.repository

import com.influxdb.query.FluxRecord
import com.influxdb.query.FluxTable
import java.time.Instant
import net.anzop.retro.model.marketData.MarketData

fun <T : MarketData> parseTable(tables: List<FluxTable>, factory: MarketDataFactory<T>): List<T> {
    return toRecords(tables).mapNotNull { (ticker, records) ->
        val fields = mutableMapOf<String, MutableList<Any?>>()
        var type: String? = null

        records
            .sortedBy { record -> record.getValueByKey("_time") as? Instant }
            .forEach { record ->
                val field = record.field ?: return@forEach
                val value = record.value ?: return@forEach

                fields["time"] = mutableListOf(record.time)
                fields["measurement"] = mutableListOf(record.measurement)
                val values = fields.getOrPut(field) { mutableListOf() }
                values.add(value)

                if (type == null) {
                    type = record.getValueByKey("type") as? String
                }
            }

        factory.create(ticker, fields)
    }
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
