package net.anzop.results

import java.time.OffsetDateTime
import java.util.UUID

trait BaseMarketData {
  val measureId: UUID
  val timestamp: OffsetDateTime
  val ticker: String
  val tags: Map[String, String]
}

trait BaseDelta extends BaseMarketData

trait BaseWindow extends BaseMarketData
