package net.anzop.processors

import net.anzop.helpers.MapExtensions.MapOps
import net.anzop.results.{QuotationDeltas, WindowedQuotations}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.scala._

class QuotationDeltaProcessor extends DeltaProcessor[WindowedQuotations, QuotationDeltas] {

  implicit override val typeInfoT: TypeInformation[WindowedQuotations] =
    createTypeInformation[WindowedQuotations]

  override def compose(prev: WindowedQuotations, curr: WindowedQuotations): QuotationDeltas =
    QuotationDeltas(
      measureId                      = curr.measureId,
      ticker                         = curr.ticker,
      timestamp                      = curr.timestamp,
      recordCountDelta               = curr.recordCount - prev.recordCount,
      minAskPriceDelta               = curr.minAskPrice - prev.minAskPrice,
      minBidPriceDelta               = curr.minBidPrice - prev.minBidPrice,
      maxAskPriceDelta               = curr.maxAskPrice - prev.maxAskPrice,
      maxBidPriceDelta               = curr.maxBidPrice - prev.maxBidPrice,
      sumAskQuantityDelta            = curr.sumAskQuantity - prev.sumAskQuantity,
      sumBidQuantityDelta            = curr.sumBidQuantity - prev.sumBidQuantity,
      sumAskNotionalDelta            = curr.sumAskNotional - prev.sumAskNotional,
      sumBidNotionalDelta            = curr.sumBidNotional - prev.sumBidNotional,
      volumeWeightedAgAskPriceDelta  = curr.volumeWeightedAgAskPrice - prev.volumeWeightedAgAskPrice,
      volumeWeightedAvgBidPriceDelta = curr.volumeWeightedAvgBidPrice - prev.volumeWeightedAvgBidPrice,
      bidAskSpreadDelta              = curr.bidAskSpread - prev.bidAskSpread,
      spreadMidpointDelta            = curr.spreadMidpoint - prev.spreadMidpoint,
      orderImbalanceDelta            = curr.orderImbalance - prev.orderImbalance,
      tags                           = prev.tags.intersect(curr.tags)
    )
}
