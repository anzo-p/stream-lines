package net.anzop.retro.repository

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point
import com.influxdb.query.dsl.Flux
import com.influxdb.query.dsl.functions.restriction.Restrictions
import java.time.Instant
import java.time.OffsetDateTime
import java.time.temporal.ChronoUnit
import net.anzop.retro.config.InfluxDBConfig
import net.anzop.retro.model.BarData
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class BarDataRepository (
    private val influxDBConfig: InfluxDBConfig,
    private val influxDBClient: InfluxDBClient
) {

    fun getLatestMeasurementTime(ticker: String): Instant? {
        val flux = Flux
            .from(influxDBConfig.bucket)
            .range(-10L, ChronoUnit.YEARS)
            .filter(
                Restrictions.and(
                    Restrictions.measurement().equal("daily_bar"),
                    Restrictions.tag("ticker").equal(ticker)
                )
            )
            .sort(true)
            .limit(1)
            .toString()

        return runQuery(flux).firstOrNull()?.records?.firstOrNull()?.time
    }

    fun saveBarData(barData: BarData) {
        val point = Point.measurement("daily_bar")
            .time(barData.marketTimestamp.toInstant().toEpochMilli(), WritePrecision.MS)
            .addTag("ticker", barData.ticker)
            .addTag("barTimeSpan", barData.barTimeSpan)
            .addTag("marketDate", barData.marketTimestamp.toLocalDate().toString())
            .addField("id", barData.id.toString())
            .addField("open", barData.openingPrice)
            .addField("close", barData.closingPrice)
            .addField("high", barData.highPrice)
            .addField("low", barData.lowPrice)
            .addField("numberOfTrades", barData.numberOfTrades)
            .addField("volume", barData.volume)
            .addField("volumeWeighted", barData.volumeWeighted)

        influxDBClient.writeApiBlocking.writePoint(point)
    }

    private fun runQuery(q: String) = influxDBClient.queryApi.query(q)
}
