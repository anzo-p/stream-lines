package net.anzop.processors

import net.anzop.Ripples.logger
import net.anzop.helpers.MathHelper
import net.anzop.results.WindowedQuotations
import net.anzop.types.{Quotation, TradeUnit}
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

class QuotationWindowProcessor[IN <: Quotation] extends WindowFunction[IN, WindowedQuotations, String, TimeWindow] {

  private def filterMad(xs: Seq[TradeUnit], z: BigDecimal = BigDecimal(4)): Seq[TradeUnit] = {
    require(xs.nonEmpty, "filterMad of empty sequence")

    val median = MathHelper.median(xs.map(_.price.amount))
    val mad    = MathHelper.mad(xs.map(_.price.amount))

    xs.collect({ case t if (t.price.amount - median).abs <= z * mad => t })
  }

  override def apply(
      key: String,
      window: TimeWindow,
      input: Iterable[IN],
      out: Collector[WindowedQuotations]
    ): Unit = {
    if (input.isEmpty) {
      logger.warn(s"QuotationWindow received empty input for key $key and window $window")
      return
    }

    val elems                      = input.toVector
    val asksMad                    = filterMad(elems.map(_.ask))
    val bidsMad                    = filterMad(elems.map(_.bid))
    val askPrices                  = asksMad.map(_.price.amount)
    val bidPrices                  = bidsMad.map(_.price.amount)
    val count                      = elems.size
    val minAskPrice: BigDecimal    = askPrices.min
    val minBidPrice: BigDecimal    = bidPrices.min
    val maxAskPrice: BigDecimal    = askPrices.max
    val maxBidPrice: BigDecimal    = bidPrices.max
    val sumAskQty: BigDecimal      = elems.map(_.ask.lotSize).sum
    val sumBidQty: BigDecimal      = elems.map(_.bid.lotSize).sum
    val sumAskNotional: BigDecimal = elems.map(q => q.ask.price.amount * q.ask.lotSize).sum
    val sumBidNotional: BigDecimal = elems.map(q => q.bid.price.amount * q.bid.lotSize).sum
    val vwapAsk: BigDecimal        = if (sumAskQty > 0) sumAskNotional / sumAskQty else BigDecimal(0)
    val vwapBid: BigDecimal        = if (sumBidQty > 0) sumBidNotional / sumBidQty else BigDecimal(0)
    val spread: BigDecimal         = vwapAsk - vwapBid
    val mid: BigDecimal            = (vwapBid + vwapAsk) / 2
    val totalQty: BigDecimal       = sumAskQty + sumBidQty
    val orderImbalance: BigDecimal = if (totalQty > 0) (sumBidQty - sumAskQty) / totalQty else BigDecimal(0)

    out.collect(
      WindowedQuotations(
        measureId                 = UUID.randomUUID(),
        ticker                    = key,
        windowStartTime           = OffsetDateTime.ofInstant(Instant.ofEpochMilli(window.getStart), ZoneOffset.UTC),
        timestamp                 = OffsetDateTime.ofInstant(Instant.ofEpochMilli(window.getEnd), ZoneOffset.UTC),
        recordCount               = count,
        minAskPrice               = minAskPrice,
        minBidPrice               = minBidPrice,
        maxAskPrice               = maxAskPrice,
        maxBidPrice               = maxBidPrice,
        sumAskQuantity            = sumAskQty,
        sumBidQuantity            = sumBidQty,
        sumAskNotional            = sumAskNotional,
        sumBidNotional            = sumBidNotional,
        volumeWeightedAgAskPrice  = vwapAsk,
        volumeWeightedAvgBidPrice = vwapBid,
        bidAskSpread              = spread,
        spreadMidpoint            = mid,
        orderImbalance            = orderImbalance,
        tags                      = Map("ticker" -> key)
      )
    )
  }
}
