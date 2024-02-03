package com.anzop.processors

import com.anzop.results.WindowedQuotationVolumes
import com.anzop.types.{CryptoQuotation, Quotation, StockQuotation}
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

class QuotationWindow[T <: Quotation] private (measurementType: WindowedVolumesMeasurement)
    extends WindowFunction[T, WindowedQuotationVolumes, String, TimeWindow] {
  override def apply(
      key: String,
      window: TimeWindow,
      input: Iterable[T],
      out: Collector[WindowedQuotationVolumes]
    ): Unit = {
    val sumBidVolume: BigDecimal        = input.map(q => q.bid.price.amount * q.bid.lotSize).sum
    val sumAskVolume: BigDecimal        = input.map(q => q.ask.price.amount * q.ask.lotSize).sum
    val count                           = input.size
    val averageBidPrice: BigDecimal     = if (count > 0) input.map(_.bid.price.amount).sum / count else 0.0
    val averageAskPrice: BigDecimal     = if (count > 0) input.map(_.ask.price.amount).sum / count else 0.0
    val bidPriceAtWindowEnd: BigDecimal = input.last.bid.price.amount
    val askPriceAtWindowEnd: BigDecimal = input.last.ask.price.amount
    out.collect(
      WindowedQuotationVolumes(
        measureId = UUID.randomUUID(),
        measurementType,
        symbol          = key,
        windowStartTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(window.getStart), ZoneOffset.UTC),
        windowEndTime   = OffsetDateTime.ofInstant(Instant.ofEpochMilli(window.getEnd), ZoneOffset.UTC),
        sumBidVolume,
        sumAskVolume,
        count,
        averageBidPrice,
        averageAskPrice,
        bidPriceAtWindowEnd,
        askPriceAtWindowEnd,
        tags = Map("symbol" -> key)
      ))
  }
}

object QuotationWindow {

  def forStockQuotation(): QuotationWindow[StockQuotation] =
    new QuotationWindow[StockQuotation](WindowedStockQuotationVolumesMeasurement)

  def forCryptoQuotation(): QuotationWindow[CryptoQuotation] =
    new QuotationWindow[CryptoQuotation](WindowedCryptoQuotationVolumesMeasurement)
}
