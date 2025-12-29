package net.anzop.processors

import net.anzop.results.WindowedQuotations
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime
import java.util.UUID

class QuotationDeltaProcessorSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BaseWindowSpec {

  private val quotationDeltaProcessor = new QuotationDeltaProcessor()
  private val window1Begins           = OffsetDateTime.now()
  private val window2Begins           = window1Begins.plusMinutes(1L)
  private val window2Ends             = window2Begins.plusMinutes(1L)

  "QuotationDeltaProcessor" should "correctly calculate trade delta" in {

    val windowedTrades1 = WindowedQuotations(
      measureId                 = UUID.randomUUID(),
      ticker                    = "ABC",
      windowStartTime           = window1Begins,
      timestamp                 = window2Ends,
      recordCount               = 3L,
      minAskPrice               = BigDecimal(99.0),
      minBidPrice               = BigDecimal(98.0),
      maxAskPrice               = BigDecimal(100.0),
      maxBidPrice               = BigDecimal(99.0),
      sumAskQuantity            = BigDecimal(30.0),
      sumBidQuantity            = BigDecimal(25.0),
      sumAskNotional            = BigDecimal(2985.0),
      sumBidNotional            = BigDecimal(2450.0),
      volumeWeightedAgAskPrice  = BigDecimal(99.5),
      volumeWeightedAvgBidPrice = BigDecimal(98.5),
      bidAskSpread              = BigDecimal(1.0),
      spreadMidpoint            = BigDecimal(98.75),
      orderImbalance            = BigDecimal(5000.0),
      tags                      = Map("ticker" -> "ABC")
    )

    val windowedTrades2 = WindowedQuotations(
      measureId                 = UUID.randomUUID(),
      ticker                    = "ABC",
      windowStartTime           = window2Begins,
      timestamp                 = window2Ends,
      recordCount               = 4L,
      minAskPrice               = BigDecimal(95.0),
      minBidPrice               = BigDecimal(94.0),
      maxAskPrice               = BigDecimal(100.0),
      maxBidPrice               = BigDecimal(99.0),
      sumAskQuantity            = BigDecimal(50.0),
      sumBidQuantity            = BigDecimal(45.0),
      sumAskNotional            = BigDecimal(4750.0),
      sumBidNotional            = BigDecimal(4455.0),
      volumeWeightedAgAskPrice  = BigDecimal(95.0),
      volumeWeightedAvgBidPrice = BigDecimal(94.5),
      bidAskSpread              = BigDecimal(1.0),
      spreadMidpoint            = BigDecimal(94.25),
      orderImbalance            = BigDecimal(4333.0),
      tags                      = Map("ticker" -> "ABC")
    )

    val result = quotationDeltaProcessor.compose(windowedTrades1, windowedTrades2)

    result.ticker shouldBe "ABC"
    result.timestamp shouldBe window2Ends
    result.recordCountDelta shouldBe 1L
    result.minAskPriceDelta shouldBe BigDecimal(95.0) - BigDecimal(99.0)
    result.minBidPriceDelta shouldBe BigDecimal(94.0) - BigDecimal(98.0)
    result.maxAskPriceDelta shouldBe BigDecimal(100.0) - BigDecimal(100.0)
    result.maxBidPriceDelta shouldBe BigDecimal(99.0) - BigDecimal(99.0)
    result.sumAskQuantityDelta shouldBe BigDecimal(50.0) - BigDecimal(30.0)
    result.sumBidQuantityDelta shouldBe BigDecimal(45.0) - BigDecimal(25.0)
    result.sumAskNotionalDelta shouldBe BigDecimal(4750.0) - BigDecimal(2985.0)
    result.sumBidNotionalDelta shouldBe BigDecimal(4455.0) - BigDecimal(2450.0)
    result.volumeWeightedAgAskPriceDelta shouldBe BigDecimal(95.0) - BigDecimal(99.5)
    result.volumeWeightedAvgBidPriceDelta shouldBe BigDecimal(94.5) - BigDecimal(98.5)
    result.bidAskSpreadDelta shouldBe BigDecimal(1.0) - BigDecimal(1.0)
    result.spreadMidpointDelta shouldBe BigDecimal(94.25) - BigDecimal(98.75)
    result.orderImbalanceDelta shouldBe BigDecimal(4333.0) - BigDecimal(5000.0)
    result.tags shouldBe Map("ticker" -> "ABC")
  }
}
