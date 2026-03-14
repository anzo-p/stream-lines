package net.anzop.processors.Drawdown.models

case class DrawdownState(
    timestamp: Long,
    drawdownLow: Double,
    drawdownAvg: Double,
    drawdownHigh: Double
  )
