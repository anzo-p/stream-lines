package net.anzop.results

import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

class QuotationDeltasSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  "QuotationDeltas" should "serialize to proper lineprotocol format" in {
    val timestamp = OffsetDateTime.now(ZoneOffset.UTC)

    val wq = QuotationDeltas(
      measureId                      = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
      ticker                         = "ABC",
      timestamp                      = timestamp,
      recordCountDelta               = 100,
      minAskPriceDelta               = BigDecimal(120.5),
      minBidPriceDelta               = BigDecimal(119.5),
      maxAskPriceDelta               = BigDecimal(125.0),
      maxBidPriceDelta               = BigDecimal(124.0),
      sumAskQuantityDelta            = BigDecimal(600),
      sumBidQuantityDelta            = BigDecimal(400),
      sumAskNotionalDelta            = BigDecimal(73800.0),
      sumBidNotionalDelta            = BigDecimal(48800.0),
      volumeWeightedAgAskPriceDelta  = BigDecimal(123.0),
      volumeWeightedAvgBidPriceDelta = BigDecimal(122.0),
      bidAskSpreadDelta              = BigDecimal(1.0),
      spreadMidpointDelta            = BigDecimal(122.5),
      orderImbalanceDelta            = BigDecimal(0.1),
      tags                           = Map("ticker" -> "ABC")
    )

    val expectedLineProtocol =
      s"${QuotationDeltas.measurement.value}," +
        "ticker=ABC" +
        " " + "measure_id=\"123e4567-e89b-12d3-a456-426614174000\"," +
        "record_count_delta=100i," +
        "min_ask_price_delta=120.5000000000," +
        "min_bid_price_delta=119.5000000000," +
        "max_ask_price_delta=125.0000000000," +
        "max_bid_price_delta=124.0000000000," +
        "sum_ask_quantity_delta=600.0000000000," +
        "sum_bid_quantity_delta=400.0000000000," +
        "sum_ask_notional_delta=73800.0000000000," +
        "sum_bid_notional_delta=48800.0000000000," +
        "volume_weighted_avg_ask_price_delta=123.0000000000," +
        "volume_weighted_avg_bid_price_delta=122.0000000000," +
        "bid_ask_spread_delta=1.0000000000," +
        "spread_midpoint_delta=122.5000000000," +
        "order_imbalance_delta=0.1000000000" +
        " " + s"${timestamp.toInstant.getEpochSecond * 1000000000L}"

    val serialized = QuotationDeltas.influxSerializer.serialize(wq)
    serialized shouldEqual expectedLineProtocol
  }
}
