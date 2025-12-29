package net.anzop.results

import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.{OffsetDateTime, ZoneOffset}
import java.util.UUID

class WindowedTradesSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  "WindowedTrades" should "serialize to proper lineprotocol format" in {
    val startTime = OffsetDateTime.now(ZoneOffset.UTC)
    val endTime   = startTime.plusMinutes(1L)

    val wq = WindowedTrades(
      measureId              = UUID.fromString("123e4567-e89b-12d3-a456-426614174000"),
      ticker                 = "ABC",
      windowStartTime        = startTime,
      timestamp              = endTime,
      recordCount            = 100,
      priceAtWindowStart     = BigDecimal(121.0),
      minPrice               = BigDecimal(119.5),
      maxPrice               = BigDecimal(125.0),
      priceAtWindowEnd       = BigDecimal(123.0),
      sumQuantity            = BigDecimal(1000),
      sumNotional            = BigDecimal(123000.0),
      volumeWeightedAvgPrice = BigDecimal(122.0),
      tags                   = Map("ticker" -> "ABC")
    )

    val expectedLineProtocol =
      s"${WindowedTrades.measurement.value}," +
        "ticker=ABC" +
        " " + "measure_id=\"123e4567-e89b-12d3-a456-426614174000\"," +
        s"window_start_time=${startTime.toInstant.getEpochSecond * 1000000000L}i," +
        s"window_end_time=${endTime.toInstant.getEpochSecond * 1000000000L}i," +
        "record_count=100i," +
        "price_at_window_start=121.0000000000," +
        "min_price=119.5000000000," +
        "max_price=125.0000000000," +
        "price_at_window_end=123.0000000000," +
        "sum_quantity=1000.0000000000," +
        "sum_notional=123000.0000000000," +
        "volume_weighted_avg_price=122.0000000000" +
        " " + s"${endTime.toInstant.getEpochSecond * 1000000000L}"

    val serialized = WindowedTrades.influxSerializer.serialize(wq)
    serialized shouldEqual expectedLineProtocol
  }
}
