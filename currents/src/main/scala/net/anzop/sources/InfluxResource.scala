package net.anzop.sources

import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory}
import net.anzop.config.InfluxDetails
import net.anzop.models.MarketData

import java.io.Serializable
import java.time.Instant
import scala.jdk.CollectionConverters._

class InfluxResource(influxDetails: InfluxDetails) extends Serializable {
  private val client: InfluxDBClient = InfluxDBClientFactory.create(
    influxDetails.sourceUrl.toString,
    influxDetails.token.toCharArray,
    influxDetails.org
  )

  private val indexQuery: String =
    s"""
       |import "math"
       |
       |fields = ["priceChangeAvg"] //, "priceChangeHigh", "priceChangeLow"]
       |
       |from(bucket: "stream-lines-daily-bars")
       |  |> range(start: -100mo, stop: now())
       |  |> filter(fn: (r) => r["_measurement"] == "ix_reg_arith_d")
       |  |> filter(fn: (r) => contains(value: r._field, set: fields))
       |  |> keep(columns: ["_time", "_value", "_field"])
       |  // results appear more accurate on true value and not on its log
       |""".stripMargin

  private val queryApi = client.getQueryApi

  private def fetch(query: String): List[MarketData] = {
    try {
      queryApi
        .query(query, influxDetails.org)
        .asScala
        .toList
        .flatMap(_.getRecords.asScala.map { record =>
          MarketData(
            timestamp = Instant.ofEpochMilli(record.getTime.toEpochMilli).toEpochMilli,
            field     = record.getValueByKey("_field").toString,
            value     = record.getValueByKey("_value").asInstanceOf[Double]
          )
        })

    } catch {
      case _: Exception =>
        List.empty
    }
  }

  def fetchIndexData(): List[MarketData] =
    fetch(indexQuery)

  def close(): Unit =
    client.close()
}
