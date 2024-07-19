package net.anzop.retro.repository.influxdb

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.WriteApi
import com.influxdb.client.write.Point
import com.influxdb.query.FluxTable
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.FilterFlux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import java.time.Instant
import java.time.temporal.ChronoUnit
import net.anzop.retro.config.InfluxDBConfig
import net.anzop.retro.model.marketData.BarData
import net.anzop.retro.model.marketData.MarketData
import net.anzop.retro.model.marketData.Measurement
import org.springframework.stereotype.Repository

@Repository
class MarketDataRepository (
    private val influxDBConfig: InfluxDBConfig,
    private val influxDBClient: InfluxDBClient,
    private val influxDBAsyncWriter: WriteApi
) {
    fun getEarliestSourceBarDataEntry(ticker: String): Instant? =
        getLastMeasurement(
            measurement = Measurement.SECURITIES_RAW_DAILY,
            ticker = ticker,
            clazz = BarData::class.java
        )?.marketTimestamp

    fun <T : MarketData> getMeasurements(
        measurement: Measurement,
        from: Instant,
        til: Instant? = null,
        ticker: String? = null,
        clazz: Class<T>
    ): List<T> {
        val tilInstant = til ?: from
        val baseQ = Flux
            .from(influxDBConfig.bucket)
            .range(
                from.truncatedTo(ChronoUnit.DAYS),
                tilInstant.truncatedTo(ChronoUnit.DAYS).plus(1, ChronoUnit.DAYS)
            )
            .filter(Restrictions.measurement().equal(measurement.code))

        val q = ticker?.let {
            baseQ.filter(Restrictions.tag("ticker").equal(it))
        } ?: baseQ

        return runAndParse(q.toString(), clazz)
            .filter { clazz.isInstance(it) }
            .map { clazz.cast(it) }
    }

    fun <T : MarketData> getLastMeasurement(
        measurement: Measurement,
        ticker: String,
        earlierThan: Instant? = null,
        clazz: Class<T>
    ): T? =
        getLatestMeasurementTime(measurement, ticker, earlierThan)
            ?.let { ts ->
                val q = Flux
                    .from(influxDBConfig.bucket)
                    .range(Instant.ofEpochMilli(0L))
                    .filter(
                        Restrictions.and(
                            Restrictions.measurement().equal(measurement.code),
                            Restrictions.time().equal(ts),
                            Restrictions.tag("ticker").equal(ticker)
                        )
                    )
                    .toString()

                val result = runAndParse(q, clazz).first()
                return cast(result, clazz)
            }

    private fun getLatestMeasurementTime(
        measurement: Measurement,
        ticker: String,
        earlierThan: Instant? = null
    ): Instant? {
        val baseQ = baseFlux(
            measurement = measurement,
            ticker = ticker,
            stop = earlierThan
        )
        val q = baseQ
            .max("_time")
            .toString()

        return runQuery(q)
            .firstOrNull()
            ?.records
            ?.firstOrNull()
            ?.time
    }

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

    fun <T> save(entity: T) =
        save(listOf(entity))

    fun <T> save(entities: List<T>) =
        influxDBClient.takeIf { entities.isNotEmpty() }
            ?.writeApiBlocking
            ?.writePoints(entities.map { toPoint(it) })

    // still takes time, must never need to await
    fun <T> saveAsync(entities: List<T>) =
        write(entities.map { toPoint(it) })

    private fun write(points: List<Point>) {
        influxDBAsyncWriter.writePoints(points)
        influxDBAsyncWriter.flush()
    }

    fun <T> runAndParse(q: String, clazz: Class<T>): List<MarketData> =
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
}
