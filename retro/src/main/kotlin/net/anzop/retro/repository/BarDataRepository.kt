package net.anzop.retro.repository

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.WriteApi
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.influxdb.query.FluxTable
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
    private val influxDBClient: InfluxDBClient,
    private val influxDBAsyncWriter: WriteApi
) {
    fun getMeasurements(measurement: Measurement, from: Instant, to: Instant? = null): List<BarData> {
        val tilDate = to ?: from
        val flux = Flux
            .from(influxDBConfig.bucket)
            .range(
                from.truncatedTo(ChronoUnit.DAYS),
                tilDate.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)
            )
            .filter(Restrictions.and(Restrictions.measurement().equal(measurement.code)))
            .toString()

        return toBarDataList(runQuery(flux))
    }

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
            .firstOrNull()
            ?.records
            ?.firstOrNull()
            ?.time
    }

    fun save(barData: BarData) =
        influxDBClient
            .writeApiBlocking
            .writePoint(toPoint(barData))

    // still takes time, must never need to await
    fun saveAsync(barData: List<BarData>) =
        write(barData.map { toPoint(it) })

    private fun toPoint(barData: BarData): Point =
        Point
            .measurement(barData.measurement.code)
            .time(barData.marketTimestamp.toInstant().toEpochMilli(), WritePrecision.MS)
            .addTag("ticker", barData.ticker)
            .addField("openingPrice", barData.openingPrice)
            .addField("closingPrice", barData.closingPrice)
            .addField("highPrice", barData.highPrice)
            .addField("lowPrice", barData.lowPrice)
            .addField("volumeWeightedAvgPrice", barData.volumeWeightedAvgPrice)
            .addField("totalTradingValue", barData.totalTradingValue)

    private fun write(points: List<Point>) {
        influxDBAsyncWriter.writePoints(points)
        influxDBAsyncWriter.flush()
    }

    private fun runQuery(q: String): List<FluxTable> =
        influxDBClient
            .queryApi
            .query(q)
}
