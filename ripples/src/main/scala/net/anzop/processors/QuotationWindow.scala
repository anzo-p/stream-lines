package net.anzop.processors

import net.anzop.Ripples.logger
import net.anzop.results.WindowedQuotes
import net.anzop.types.{CryptoQuotation, Quotation, StockQuotation}
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

class QuotationWindow[T <: Quotation] private (measurementType: WindowedMeasurement)
    extends WindowFunction[T, WindowedQuotes, String, TimeWindow] {

  override def apply(
      key: String,
      window: TimeWindow,
      input: Iterable[T],
      out: Collector[WindowedQuotes]
    ): Unit = {
    if (input.isEmpty) {
      logger.warn(s"QuotationWindow received empty input for key $key and window $window")
      return
    }

    val elems                                 = input.toVector
    val asks                                  = elems.map(_.ask.price.amount)
    val bids                                  = elems.map(_.bid.price.amount)
    val count                                 = elems.size
    val askPriceAtWindowStart: BigDecimal     = elems.head.ask.price.amount
    val bidPriceAtWindowStart: BigDecimal     = elems.head.bid.price.amount
    val minAskPrice: BigDecimal               = asks.min
    val minBidPrice: BigDecimal               = bids.min
    val maxAskPrice: BigDecimal               = asks.max
    val maxBidPrice: BigDecimal               = bids.max
    val last                                  = elems.last
    val bidPriceAtWindowEnd: BigDecimal       = last.bid.price.amount
    val askPriceAtWindowEnd: BigDecimal       = last.ask.price.amount
    val sumAskQuantity: BigDecimal            = elems.map(_.ask.lotSize).sum
    val sumBidQuantity: BigDecimal            = elems.map(_.bid.lotSize).sum
    val sumAskNotional: BigDecimal            = elems.map(q => q.ask.price.amount * q.ask.lotSize).sum
    val sumBidNotional: BigDecimal            = elems.map(q => q.bid.price.amount * q.bid.lotSize).sum
    val volumeWeightedAvgAskPrice: BigDecimal = if (sumAskQuantity > 0) sumAskNotional / sumAskQuantity else BigDecimal(0)
    val volumeWeightedAvgBidPrice: BigDecimal = if (sumBidQuantity > 0) sumBidNotional / sumBidQuantity else BigDecimal(0)

    out.collect(
      WindowedQuotes(
        measureId = UUID.randomUUID(),
        measurementType,
        symbol                    = key,
        windowStartTime           = OffsetDateTime.ofInstant(Instant.ofEpochMilli(window.getStart), ZoneOffset.UTC),
        windowEndTime             = OffsetDateTime.ofInstant(Instant.ofEpochMilli(window.getEnd), ZoneOffset.UTC),
        recordCount               = count,
        askPriceAtWindowStart     = askPriceAtWindowStart,
        bidPriceAtWindowStart     = bidPriceAtWindowStart,
        minAskPrice               = minAskPrice,
        minBidPrice               = minBidPrice,
        maxAskPrice               = maxAskPrice,
        maxBidPrice               = maxBidPrice,
        askPriceAtWindowEnd       = askPriceAtWindowEnd,
        bidPriceAtWindowEnd       = bidPriceAtWindowEnd,
        sumAskQuantity            = sumAskQuantity,
        sumBidQuantity            = sumBidQuantity,
        sumAskNotional            = sumAskNotional,
        sumBidNotional            = sumBidNotional,
        volumeWeightedAgAskPrice  = volumeWeightedAvgAskPrice,
        volumeWeightedAvgBidPrice = volumeWeightedAvgBidPrice,
        tags                      = Map("ticker" -> key)
      )
    )
  }
}

object QuotationWindow {

  def forStockQuotation(): QuotationWindow[StockQuotation] =
    new QuotationWindow[StockQuotation](WindowedStockQuotesMeasurement)

  def forCryptoQuotation(): QuotationWindow[CryptoQuotation] =
    new QuotationWindow[CryptoQuotation](WindowedCryptoQuotesMeasurement)
}
