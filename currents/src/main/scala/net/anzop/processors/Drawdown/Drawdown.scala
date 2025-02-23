package net.anzop.processors.Drawdown

import net.anzop.helpers.DateHelpers.epochToStringDate
import net.anzop.sinks.influxdb.InfluxSerializable

case class Drawdown(
    timestamp: Long,
    field: String,
    value: Double,
    drawdown: Double
  ) extends InfluxSerializable {

  override val measurement: String = "drawdown"

  override def fields: Map[String, Any] =
    Map("value" -> value, "drawdown" -> drawdown)

  override def tags: Map[String, String] =
    Map("type" -> "drawdown", "field" -> field)

  override def toString: String =
    s"""DrawdownData(
       |timestamp: ${epochToStringDate(timestamp)},
       |field: $field,
       |value: $value,
       |drawdown: $drawdown)
       |""".stripMargin
}
