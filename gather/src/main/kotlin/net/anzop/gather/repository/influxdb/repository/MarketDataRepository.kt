package net.anzop.gather.repository.influxdb.repository

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.WriteApi
import com.influxdb.client.write.Point
import com.influxdb.query.FluxTable
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.FilterFlux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import java.time.Instant
import net.anzop.gather.config.InfluxDBConfig
import net.anzop.gather.helpers.date.plusOneDayAlmost
import net.anzop.gather.model.marketData.MarketData
import net.anzop.gather.model.marketData.Measurement
import org.springframework.stereotype.Repository

@Repository
class MarketDataRepository (
    private val influxDBConfig: InfluxDBConfig,
    private val influxDBClient: InfluxDBClient,
    private val influxDBAsyncWriter: WriteApi
) {
    fun <T : MarketData> getMeasurements(
        measurement: Measurement,
        from: Instant,
        til: Instant? = null,
        ticker: String? = null,
        clazz: Class<T>
    ): List<T> {
        val tilInstant = til ?: from.plusOneDayAlmost()

        val baseQ = Flux
            .from(influxDBConfig.bucket)
            .range(from, tilInstant)
            .filter(Restrictions.measurement().equal(measurement.code))

        val q = ticker?.let {
            baseQ.filter(Restrictions.tag("ticker").equal(it))
        } ?: baseQ

        return runAndParse(q.toString(), clazz)
            .filter { clazz.isInstance(it) }
            .map { clazz.cast(it) }
    }

    fun <T : MarketData> getFirstMeasurement(
        measurement: Measurement,
        ticker: String,
        since: Instant? = null,
        clazz: Class<T>
    ): T? =
        getFirstMeasurementTime(
            measurement = measurement,
            ticker = ticker,
            since ?: Instant.ofEpochMilli(0L)
        )
            ?.let { ts ->
                val q = baseFlux(
                    measurement = measurement,
                    ticker = ticker,
                    start = ts
                ).filter(Restrictions.time().equal(ts))

                val result = runAndParse(q.toString(), clazz).first()
                return cast(result, clazz)
            }

    fun getFirstMeasurementTime(
        measurement: Measurement,
        ticker: String,
        since: Instant? = null
    ): Instant? {
        val q = baseFlux(
            measurement = measurement,
            ticker = ticker,
            start = since
        ).min("_time")

        return queryForTimestamp(q)
    }

    fun getLatestMeasurementTime(
        measurement: Measurement,
        ticker: String,
        earlierThan: Instant? = null,
        regularTradingHours: Boolean? = true,
    ): Instant? {
        val baseQ = baseFlux(
            measurement = measurement,
            ticker = ticker,
            stop = earlierThan
        ).max("_time")

        val q = regularTradingHours?.let {
            baseQ.filter(Restrictions.tag("regularTradingHours").equal(it.toString()))
        } ?: baseQ

        return queryForTimestamp(q)
    }

    fun <T> save(entities: List<T>) =
        influxDBClient.takeIf { entities.isNotEmpty() }
            ?.writeApiBlocking
            ?.writePoints(entities.map { toPoint(it) })

    fun <T> saveAsync(entities: List<T>) =
        write(entities.map { toPoint(it) })

    private fun baseFlux(
        measurement: Measurement,
        ticker: String,
        start: Instant? = null,
        stop: Instant? = null,
    ): FilterFlux =
        Flux
            .from(influxDBConfig.bucket)
            .range(
                start ?: Instant.ofEpochMilli(0L),
                stop ?: Instant.now()
            )
            .filter(
                Restrictions.and(
                    Restrictions.measurement().equal(measurement.code),
                    Restrictions.tag("ticker").equal(ticker)
                )
            )

    private fun queryForTimestamp(q: Flux): Instant? =
        runQuery(q.toString())
            .firstOrNull()
            ?.records
            ?.firstOrNull()
            ?.time

    private fun <T> runAndParse(q: String, clazz: Class<T>): List<MarketData> =
        parseTable(
            tables = runQuery(q),
            factory = factories[clazz.simpleName] ?: throw IllegalArgumentException("Invalid class type: $clazz")
        )

    private fun runQuery(q: String): List<FluxTable> =
        influxDBClient
            .queryApi
            .query(q)

    private fun <T> cast(result: MarketData, clazz: Class<T>): T =
        if (clazz.isInstance(result)) {
            clazz.cast(result)
        } else {
            throw IllegalArgumentException("Invalid class type: $clazz")
        }

    private fun write(points: List<Point>) {
        influxDBAsyncWriter.writePoints(points)
        influxDBAsyncWriter.flush()
    }
}
