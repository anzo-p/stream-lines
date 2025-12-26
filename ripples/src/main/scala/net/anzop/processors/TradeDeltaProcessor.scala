package net.anzop.processors

import net.anzop.results.{TradeDeltas, WindowedTrades}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.scala.createTypeInformation

class TradeDeltaProcessor extends DeltaProcessor[WindowedTrades, TradeDeltas] {

  implicit override val typeInfoT: TypeInformation[WindowedTrades] =
    createTypeInformation[WindowedTrades]

  override def compose(prev: WindowedTrades, curr: WindowedTrades): TradeDeltas =
    TradeDeltas(
      measureId                   = curr.measureId,
      ticker                      = curr.ticker,
      timestamp                   = curr.timestamp,
      recordCountDelta            = curr.recordCount - prev.recordCount,
      minPriceDelta               = curr.minPrice - prev.minPrice,
      maxPriceDelta               = curr.maxPrice - prev.maxPrice,
      sumQuantityDelta            = curr.sumQuantity - prev.sumQuantity,
      sumNotionalDelta            = curr.sumNotional - prev.sumNotional,
      volumeWeightedAvgPriceDelta = curr.volumeWeightedAvgPrice - prev.volumeWeightedAvgPrice,
      tags                        = curr.tags
    )
}
