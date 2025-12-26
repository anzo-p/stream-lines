package net.anzop.processors

import net.anzop.results.{QuotationDeltas, WindowedQuotes}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.api.scala._

class QuotationDeltaProcessor extends DeltaProcessor[WindowedQuotes, QuotationDeltas] {

  implicit override val typeInfoT: TypeInformation[WindowedQuotes] =
    createTypeInformation[WindowedQuotes]

  override protected def compose(prev: WindowedQuotes, curr: WindowedQuotes): QuotationDeltas =
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
      orderBookImbalanceDelta        = curr.orderBookImbalance - prev.orderBookImbalance,
      tags                           = curr.tags
    )
}
