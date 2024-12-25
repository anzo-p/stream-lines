package net.anzop.processors.Drawdown

import net.anzop.helpers.DateHelpers.epochToStringDate

case class Drawdown(
    timestamp: Long,
    field: String,
    value: Double,
    drawdown: Double
  ) {

  override def toString: String =
    s"""DrawdownData(
       |timestamp: ${epochToStringDate(timestamp)},
       |field: $field,
       |value: $value,
       |drawdown: $drawdown)
       |""".stripMargin
}
