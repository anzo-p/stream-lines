package net.anzop.models

import breeze.storage.Zero

case class MarketData(timestamp: Long, field: String, value: Double)

object MarketData {
  implicit val marketDataRecordZero: Zero[MarketData] = Zero(MarketData(0L, "", 0.0))
}
