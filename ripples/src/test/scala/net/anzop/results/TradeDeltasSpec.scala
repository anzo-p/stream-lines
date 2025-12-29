package net.anzop.results

import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

class TradeDeltasSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  "TradeDeltas" should "serialize to proper lineprotocol format" in {
    val timestamp = OffsetDateTime.now(ZoneOffset.UTC)

    val wq = TradeDeltas(
      measureId                   = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
      ticker                      = "ABC",
      timestamp                   = timestamp,
      recordCountDelta            = 100,
      minPriceDelta               = BigDecimal("120.5"),
      maxPriceDelta               = BigDecimal("125.0"),
      sumQuantityDelta            = BigDecimal("1000.0"),
      sumNotionalDelta            = BigDecimal("122600.0"),
      volumeWeightedAvgPriceDelta = BigDecimal("123.0"),
      tags                        = Map("ticker" -> "ABC")
    )

    val expectedLineProtocol =
      s"${TradeDeltas.measurement.value}," +
        "ticker=ABC" +
        " " + "measure_id=\"123e4567-e89b-12d3-a456-426614174000\"," +
        "record_count_delta=100i," +
        "min_price_delta=120.5000000000," +
        "max_price_delta=125.0000000000," +
        "sum_quantity_delta=1000.0000000000," +
        "sum_notional_delta=122600.0000000000," +
        "volume_weighted_avg_price_delta=123.0000000000" +
        " " + s"${timestamp.toInstant.getEpochSecond * 1000000000L}"

    val serialized = TradeDeltas.influxSerializer.serialize(wq)
    serialized shouldEqual expectedLineProtocol
  }
}
