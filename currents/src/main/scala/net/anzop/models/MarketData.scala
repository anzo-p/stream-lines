package net.anzop.models

import breeze.storage.Zero

case class MarketData(
    timestamp: Long,
    priceChangeLow: Double,
    priceChangeAvg: Double,
    priceChangeHigh: Double
  )

object MarketData {
  implicit val marketDataRecordZero: Zero[MarketData] = Zero(MarketData(0L, 0.0, 0.0, 0.0))
}
