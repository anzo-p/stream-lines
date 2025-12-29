package net.anzop.results

import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

class WindowedQuotationsSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  "WindowedQuotes" should "serialize to proper lineprotocol format" in {
    val startTime = OffsetDateTime.now(ZoneOffset.UTC)
    val endTime   = startTime.plusMinutes(1L)

    val wq = WindowedQuotations(
      measureId                 = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
      ticker                    = "ABC",
      windowStartTime           = startTime,
      timestamp                 = endTime,
      recordCount               = 100,
      minAskPrice               = BigDecimal(120.5),
      minBidPrice               = BigDecimal(119.5),
      maxAskPrice               = BigDecimal(125.0),
      maxBidPrice               = BigDecimal(124.0),
      sumAskQuantity            = BigDecimal(600),
      sumBidQuantity            = BigDecimal(400),
      sumAskNotional            = BigDecimal(73800.0),
      sumBidNotional            = BigDecimal(48800.0),
      volumeWeightedAgAskPrice  = BigDecimal(123.0),
      volumeWeightedAvgBidPrice = BigDecimal(122.0),
      bidAskSpread              = BigDecimal(1.0),
      spreadMidpoint            = BigDecimal(122.5),
      orderImbalance            = BigDecimal(0.1),
      tags                      = Map("ticker" -> "ABC")
    )

    val expectedLineProtocol =
      s"${WindowedQuotations.measurement.value}," +
        "ticker=ABC" +
        " " + "measure_id=\"123e4567-e89b-12d3-a456-426614174000\"," +
        s"window_start_time=${startTime.toInstant.getEpochSecond * 1000000000L}i," +
        s"window_end_time=${endTime.toInstant.getEpochSecond * 1000000000L}i," +
        "record_count=100i," +
        "min_ask_price=120.5000000000," +
        "min_bid_price=119.5000000000," +
        "max_ask_price=125.0000000000," +
        "max_bid_price=124.0000000000," +
        "sum_ask_quantity=600.0000000000," +
        "sum_bid_quantity=400.0000000000," +
        "sum_ask_notional=73800.0000000000," +
        "sum_bid_notional=48800.0000000000," +
        "volume_weighted_avg_ask_price=123.0000000000," +
        "volume_weighted_avg_bid_price=122.0000000000," +
        "bid_ask_spread=1.0000000000," +
        "spread_midpoint=122.5000000000," +
        "order_imbalance=0.1000000000" +
        " " + s"${endTime.toInstant.getEpochSecond * 1000000000L}"

    val serialized = WindowedQuotations.influxSerializer.serialize(wq)
    serialized shouldEqual expectedLineProtocol
  }
}
