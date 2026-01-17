package net.anzop.sources.marketData

import com.influxdb.query.{FluxRecord, FluxTable}
import net.anzop.models.MarketData

import java.time.Instant
import scala.jdk.CollectionConverters._

sealed trait DatasetMapping[T] {
  def conditions: String
  def recordMapper: FluxRecord => T

  final def query(params: QueryParams): String = {
    val start = params.start.getOrElse(1)
    val stop  = params.stop.getOrElse(Instant.now().getEpochSecond)
    s"""
       #from(bucket: "${params.bucket}")
       # |> range(start: $start, stop: $stop)
       # |> filter(fn: (r) => r["_measurement"] == "${params.measurement}")
       # |> sort(columns: ["_time"], desc: false)
       # $conditions
       #""".stripMargin('#')
  }

  final def tableMapper: FluxTable => List[T] = table =>
    table
      .getRecords
      .asScala
      .map(recordMapper)
      .toList
}

case object GetLatestTrendEntry extends DatasetMapping[Long] {
  override def conditions: String =
    s"""
       # |> filter(fn: (r) => r["open"] == "false")
       # |> filter(fn: (r) => r["_field"] == "regression_slope")
       # |> last()
       # |> keep(columns: ["_time"])
       #""".stripMargin('#')

  override def recordMapper: FluxRecord => Long =
    _.getTime.getEpochSecond
}

case object GetIndexData extends DatasetMapping[MarketData] {
  override def conditions: String =
    s"""
       # |> filter(fn: (r) => contains(value: r._field, set: ["priceChangeLow", "priceChangeAvg", "priceChangeHigh"]))
       # |> pivot(rowKey:["_time"], columnKey: ["_field"], valueColumn: "_value")
       # |> keep(columns: ["_time","priceChangeLow","priceChangeAvg","priceChangeHigh"])
       #""".stripMargin('#')

  override def recordMapper: FluxRecord => MarketData =
    record =>
      MarketData(
        timestamp       = Instant.ofEpochMilli(record.getTime.toEpochMilli).toEpochMilli,
        priceChangeLow  = record.getValueByKey("priceChangeLow").asInstanceOf[Double],
        priceChangeAvg  = record.getValueByKey("priceChangeAvg").asInstanceOf[Double],
        priceChangeHigh = record.getValueByKey("priceChangeHigh").asInstanceOf[Double]
      )
}
