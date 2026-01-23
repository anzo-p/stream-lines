package net.anzop.processors.Trend.models

import net.anzop.models.MarketData
import net.anzop.models.Types.DV

case class TrendDiscovery(discovered: List[TrendSegment], undecidedTail: Option[TrendSegment], tailData: DV[MarketData])

object TrendDiscovery {
  def boomerang(chunk: DV[MarketData]) = TrendDiscovery(Nil, None, chunk)
}
