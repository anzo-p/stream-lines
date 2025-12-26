package net.anzop.processors

import net.anzop.Ripples.logger
import net.anzop.results.WindowedTrades
import net.anzop.types.Trade
import org.apache.flink.streaming.api.scala.function.WindowFunction
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector

import java.time.{Instant, OffsetDateTime, ZoneOffset}
import java.util.UUID

class TradeWindowProcessor[IN <: Trade] extends WindowFunction[IN, WindowedTrades, String, TimeWindow] {

  override def apply(
      key: String,
      window: TimeWindow,
      input: Iterable[IN],
      out: Collector[WindowedTrades]
    ): Unit = {
    if (input.isEmpty) {
      logger.warn(s"TradeWindow received empty input for key $key and window $window")
      return
    }

    val elems                              = input.toVector
    val prices                             = elems.map(_.settle.price.amount)
    val count                              = elems.size
    val priceAtWindowStart: BigDecimal     = elems.head.settle.price.amount
    val minPrice: BigDecimal               = prices.min
    val maxPrice: BigDecimal               = prices.max
    val priceAtWindowEnd: BigDecimal       = elems.last.settle.price.amount
    val sumQuantity: BigDecimal            = elems.map(_.settle.lotSize).sum
    val sumNotional: BigDecimal            = elems.map(t => t.settle.price.amount * t.settle.lotSize).sum
    val volumeWeightedAvgPrice: BigDecimal = if (sumQuantity == 0) BigDecimal(0) else sumNotional / sumQuantity

    out.collect(
      WindowedTrades(
        measureId              = UUID.randomUUID(),
        ticker                 = key,
        windowStartTime        = OffsetDateTime.ofInstant(Instant.ofEpochMilli(window.getStart), ZoneOffset.UTC),
        timestamp              = OffsetDateTime.ofInstant(Instant.ofEpochMilli(window.getEnd), ZoneOffset.UTC),
        recordCount            = count,
        priceAtWindowStart     = priceAtWindowStart,
        minPrice               = minPrice,
        maxPrice               = maxPrice,
        priceAtWindowEnd       = priceAtWindowEnd,
        sumQuantity            = sumQuantity,
        sumNotional            = sumNotional,
        volumeWeightedAvgPrice = volumeWeightedAvgPrice,
        tags                   = Map("ticker" -> key)
      )
    )
  }
}
