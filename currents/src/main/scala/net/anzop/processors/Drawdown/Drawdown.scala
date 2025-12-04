package net.anzop.processors.Drawdown

import net.anzop.helpers.DateAndTimeHelpers.epochToStringDate
import net.anzop.sinks.influxdb.InfluxSerializable

case class Drawdown(
    timestamp: Long,
    priceChangeLow: Double,
    priceChangeAvg: Double,
    priceChangeHigh: Double,
    drawdownLow: Double,
    drawdownAvg: Double,
    drawdownHigh: Double
  ) extends InfluxSerializable {

  override val measurement: String = "drawdown-analysis"

  override def fields: Map[String, Any] =
    Map(
      "priceChangeLow"  -> priceChangeLow,
      "priceChangeAvg"  -> priceChangeAvg,
      "priceChangeHigh" -> priceChangeHigh,
      "drawdownLow"     -> drawdownLow,
      "drawdownAvg"     -> drawdownAvg,
      "drawdownHigh"    -> drawdownHigh
    )

  override def tags: Map[String, String] =
    Map("type" -> "drawdown")

  override def toString: String =
    s"""DrawdownData(
       |timestamp: ${epochToStringDate(timestamp)},
       |priceChangeLow: $priceChangeLow,
       |priceChangeAvg: $priceChangeAvg,
       |priceChangeHigh: $priceChangeHigh,
       |drawdownLow: $drawdownLow,
       |drawdownAvg: $drawdownAvg
       |drawdownHigh: $drawdownHigh),
       |""".stripMargin
}
