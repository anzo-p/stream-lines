package net.anzop.processors

import net.anzop.results.WindowedTrades
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime
import java.util.UUID

class TradeDeltaProcessorSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BaseWindowSpec {

  private val tradeDeltaProcessor = new TradeDeltaProcessor()
  private val window1Begins       = OffsetDateTime.now()
  private val window2Begins       = window1Begins.plusMinutes(1L)
  private val window2Ends         = window2Begins.plusMinutes(1L)

  "TradeDeltaProcessor" should "correctly calculate trade delta" in {

    val windowedTrades1 = WindowedTrades(
      measureId              = UUID.randomUUID(),
      ticker                 = "ABC",
      windowStartTime        = window1Begins,
      timestamp              = window2Begins,
      recordCount            = 3L,
      priceAtWindowStart     = BigDecimal(100.0),
      minPrice               = BigDecimal(99.0),
      maxPrice               = BigDecimal(100.0),
      priceAtWindowEnd       = BigDecimal(99.0),
      sumQuantity            = BigDecimal(30.0),
      sumNotional            = BigDecimal(2985.0),
      volumeWeightedAvgPrice = BigDecimal(99.5),
      tags                   = Map("ticker" -> "ABC")
    )

    val windowedTrades2 = WindowedTrades(
      measureId              = UUID.randomUUID(),
      ticker                 = "ABC",
      windowStartTime        = window2Begins,
      timestamp              = window2Ends,
      recordCount            = 4L,
      priceAtWindowStart     = BigDecimal(99.0),
      minPrice               = BigDecimal(95.0),
      maxPrice               = BigDecimal(100.0),
      priceAtWindowEnd       = BigDecimal(95.0),
      sumQuantity            = BigDecimal(50.0),
      sumNotional            = BigDecimal(4750.0),
      volumeWeightedAvgPrice = BigDecimal(95.0),
      tags                   = Map("ticker" -> "ABC")
    )

    val result = tradeDeltaProcessor.compose(windowedTrades1, windowedTrades2)

    result.ticker shouldBe "ABC"
    result.timestamp shouldBe window2Ends
    result.recordCountDelta shouldBe 1L
    result.minPriceDelta shouldBe BigDecimal(95.0) - BigDecimal(99.0)
    result.maxPriceDelta shouldBe BigDecimal(100.0) - BigDecimal(100.0)
    result.sumQuantityDelta shouldBe BigDecimal(50.0) - BigDecimal(30.0)
    result.sumNotionalDelta shouldBe BigDecimal(4750.0) - BigDecimal(2985.0)
    result.volumeWeightedAvgPriceDelta shouldBe BigDecimal(95.0) - BigDecimal(99.5)
    result.tags shouldBe Map("ticker" -> "ABC")
  }
}
