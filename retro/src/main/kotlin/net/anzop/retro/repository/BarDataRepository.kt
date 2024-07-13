package net.anzop.retro.repository

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.influxdb.query.FluxRecord
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import java.time.Instant
import java.time.temporal.ChronoUnit
import net.anzop.retro.config.InfluxDBConfig
import net.anzop.retro.model.BarData
import net.anzop.retro.model.Measurement
import org.springframework.stereotype.Repository

@Repository
class BarDataRepository (
    private val influxDBConfig: InfluxDBConfig,
    private val influxDBClient: InfluxDBClient
) {

    fun getLatestMeasurementTime(measurement: Measurement, ticker: String): Instant? {
        val flux = Flux
            .from(influxDBConfig.bucket)
            .range(-10L, ChronoUnit.YEARS)
            .filter(
                Restrictions.and(
                    Restrictions.measurement().equal(measurement.code),
                    Restrictions.tag("ticker").equal(ticker)
                )
            )
            .max("_time")
            .toString()

        return runQuery(flux)
            ?.firstOrNull()
            ?.time
    }

    fun save(barData: BarData) {
        val point = Point
            .measurement(barData.measurement.code)
            .time(barData.marketTimestamp.toInstant().toEpochMilli(), WritePrecision.MS)
            .addTag("ticker", barData.ticker)
            .addField("openingPrice", barData.openingPrice)
            .addField("closingPrice", barData.closingPrice)
            .addField("highPrice", barData.highPrice)
            .addField("lowPrice", barData.lowPrice)
            .addField("volumeWeightedAvgPrice", barData.volumeWeightedAvgPrice)

        influxDBClient.writeApiBlocking.writePoint(point)
    }

    private fun runQuery(q: String): List<FluxRecord>? =
        influxDBClient
            .queryApi
            .query(q)
            .firstOrNull()
            ?.records
}
