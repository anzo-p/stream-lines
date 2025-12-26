package net.anzop.processors

import net.anzop.Ripples.logger
import net.anzop.results.WindowedQuotes
import net.anzop.types.Quotation
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

class QuotationWindowProcessor[IN <: Quotation] extends WindowFunction[IN, WindowedQuotes, String, TimeWindow] {

  override def apply(
      key: String,
      window: TimeWindow,
      input: Iterable[IN],
      out: Collector[WindowedQuotes]
    ): Unit = {
    if (input.isEmpty) {
      logger.warn(s"QuotationWindow received empty input for key $key and window $window")
      return
    }

    val elems                           = input.toVector
    val asks                            = elems.map(_.ask.price.amount)
    val bids                            = elems.map(_.bid.price.amount)
    val count                           = elems.size
    val askPriceAtStart: BigDecimal     = elems.head.ask.price.amount
    val bidPriceAtStart: BigDecimal     = elems.head.bid.price.amount
    val minAskPrice: BigDecimal         = asks.min
    val minBidPrice: BigDecimal         = bids.min
    val maxAskPrice: BigDecimal         = asks.max
    val maxBidPrice: BigDecimal         = bids.max
    val last                            = elems.last
    val bidPriceAtWindowEnd: BigDecimal = last.bid.price.amount
    val askPriceAtWindowEnd: BigDecimal = last.ask.price.amount
    val sumAskQty: BigDecimal           = elems.map(_.ask.lotSize).sum
    val sumBidQty: BigDecimal           = elems.map(_.bid.lotSize).sum
    val sumAskNotional: BigDecimal      = elems.map(q => q.ask.price.amount * q.ask.lotSize).sum
    val sumBidNotional: BigDecimal      = elems.map(q => q.bid.price.amount * q.bid.lotSize).sum
    val vwapAsk: BigDecimal             = if (sumAskQty > 0) sumAskNotional / sumAskQty else BigDecimal(0)
    val vwapBid: BigDecimal             = if (sumBidQty > 0) sumBidNotional / sumBidQty else BigDecimal(0)
    val spread: BigDecimal              = vwapAsk - vwapBid
    val mid: BigDecimal                 = (vwapBid + vwapAsk) / 2
    val qtyDiff: BigDecimal             = sumBidQty - sumAskQty
    val totalQty: BigDecimal            = sumAskQty + sumBidQty
    val imbalance: BigDecimal           = if (totalQty > 0) qtyDiff / totalQty else BigDecimal(0)

    out.collect(
      WindowedQuotes(
        measureId                 = UUID.randomUUID(),
        ticker                    = key,
        windowStartTime           = OffsetDateTime.ofInstant(Instant.ofEpochMilli(window.getStart), ZoneOffset.UTC),
        timestamp                 = OffsetDateTime.ofInstant(Instant.ofEpochMilli(window.getEnd), ZoneOffset.UTC),
        recordCount               = count,
        askPriceAtWindowStart     = askPriceAtStart,
        bidPriceAtWindowStart     = bidPriceAtStart,
        minAskPrice               = minAskPrice,
        minBidPrice               = minBidPrice,
        maxAskPrice               = maxAskPrice,
        maxBidPrice               = maxBidPrice,
        askPriceAtWindowEnd       = askPriceAtWindowEnd,
        bidPriceAtWindowEnd       = bidPriceAtWindowEnd,
        sumAskQuantity            = sumAskQty,
        sumBidQuantity            = sumBidQty,
        sumAskNotional            = sumAskNotional,
        sumBidNotional            = sumBidNotional,
        volumeWeightedAgAskPrice  = vwapAsk,
        volumeWeightedAvgBidPrice = vwapBid,
        bidAskSpread              = spread,
        spreadMidpoint            = mid,
        orderBookImbalance        = imbalance,
        tags                      = Map("ticker" -> key)
      )
    )
  }
}
