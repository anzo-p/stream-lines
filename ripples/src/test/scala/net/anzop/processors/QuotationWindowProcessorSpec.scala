package net.anzop.processors

import net.anzop.results.WindowedQuotations
import net.anzop.types.{Money, StockQuotation, StockTradeUnit}
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime

class QuotationWindowProcessorSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BaseWindowSpec {

  private val stockQuotationWindow = new QuotationWindowProcessor[StockQuotation]()
  private val earlier              = OffsetDateTime.now()
  private val later                = earlier.plusSeconds(1)

  "QuotationWindowProcessor" should "correctly calculate stock quotation window" in {

    val stockQuotation1 = StockQuotation(
      symbol = "AAPL",
      ask = StockTradeUnit(
        exchange = "X",
        price    = Money(100.0, "USD"),
        lotSize  = 15L
      ),
      bid = StockTradeUnit(
        exchange = "X",
        price    = Money(90.0, "USD"),
        lotSize  = 10L
      ),
      marketTimestamp = earlier,
      ingestTimestamp = earlier,
      conditions      = List(),
      tape            = "X"
    )

    val stockQuotation2 = StockQuotation(
      symbol = "AAPL",
      ask = StockTradeUnit(
        exchange = "X",
        price    = Money(99.0, "USD"),
        lotSize  = 15L
      ),
      bid = StockTradeUnit(
        exchange = "X",
        price    = Money(95.0, "USD"),
        lotSize  = 10L
      ),
      marketTimestamp = later,
      ingestTimestamp = later,
      conditions      = List(),
      tape            = "X"
    )

    val window    = new TimeWindow(0, 1000)
    val collector = new ListCollector[WindowedQuotations]

    stockQuotationWindow.apply("AAPL", window, List(stockQuotation1, stockQuotation2), collector)

    val result: List[WindowedQuotations] = collector.getResults

    result.size should be(1)
    result.head.ticker should be("AAPL")
    result.head.recordCount should be(2L)
    result.head.minAskPrice should be(99.0)
    result.head.maxAskPrice should be(100.0)
    result.head.minBidPrice should be(90.0)
    result.head.maxBidPrice should be(95.0)
    result.head.sumAskQuantity should be(30.0)
    result.head.sumBidQuantity should be(20.0)
    result.head.sumAskNotional should be(2985.0)
    result.head.sumBidNotional should be(1850.0)
    result.head.volumeWeightedAgAskPrice should be(99.5)
    result.head.volumeWeightedAvgBidPrice should be(92.5)
    result.head.bidAskSpread should be(7.0)
    result.head.spreadMidpoint should be(96.0)
    result.head.orderImbalance should be(-0.2)
  }
}
