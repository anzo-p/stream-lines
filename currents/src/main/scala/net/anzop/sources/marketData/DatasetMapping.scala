package net.anzop.sources.marketData

import com.influxdb.query.FluxTable
import net.anzop.models.MarketData

import java.time.Instant
import scala.jdk.CollectionConverters._

sealed trait DatasetMapping[T] {
  def query: String
  def mapper: FluxTable => List[T]
}

case object IndexData extends DatasetMapping[MarketData] {
  override def query: String =
    s"""
       |import "math"
       |  
       |fields = ["priceChangeAvg"]
       |
       |from(bucket: "stream-lines-daily-bars")
       |  |> range(start: -100mo, stop: now())
       |  |> filter(fn: (r) => r["_measurement"] == "ix_reg_arith_d")
       |  |> filter(fn: (r) => contains(value: r._field, set: fields))
       |  |> keep(columns: ["_time", "_value", "_field"])
       |""".stripMargin

  override def mapper: FluxTable => List[MarketData] = table =>
    table
      .getRecords
      .asScala
      .map { record =>
        MarketData(
          timestamp = Instant.ofEpochMilli(record.getTime.toEpochMilli).toEpochMilli,
          field     = record.getValueByKey("_field").toString,
          value     = record.getValueByKey("_value").asInstanceOf[Double]
        )
      }
      .toList
}
