package net.anzop.processors

import net.anzop.Ripples.logger
import net.anzop.results
import net.anzop.results.WindowedQuotationVolumes
import net.anzop.types.{CryptoQuotation, Quotation, StockQuotation}
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
    if (input.isEmpty) {
      logger.warn(s"QuotationWindow received empty input for key $key and window $window")
      return
    }

    val elems                           = input.toVector
    val count                           = elems.size
    val sumBidVolume: BigDecimal        = elems.map(q => q.bid.price.amount * q.bid.lotSize).sum
    val sumAskVolume: BigDecimal        = elems.map(q => q.ask.price.amount * q.ask.lotSize).sum
    val averageBidPrice: BigDecimal     = if (count > 0) elems.map(_.bid.price.amount).sum / count else 0.0
    val averageAskPrice: BigDecimal     = if (count > 0) elems.map(_.ask.price.amount).sum / count else 0.0
    val last                            = elems.last
    val bidPriceAtWindowEnd: BigDecimal = last.bid.price.amount
    val askPriceAtWindowEnd: BigDecimal = last.ask.price.amount
    out.collect(
      results.WindowedQuotationVolumes(
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
