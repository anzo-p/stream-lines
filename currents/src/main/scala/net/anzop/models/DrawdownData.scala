package net.anzop.models

import net.anzop.helpers.DateHelpers.epochToStringDate

case class DrawdownData(
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
