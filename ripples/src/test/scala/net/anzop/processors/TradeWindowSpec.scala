package net.anzop.processors

import net.anzop.results.WindowedTrades
import net.anzop.types.{Money, StockTrade, StockTradeUnit}
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime

class TradeWindowSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll with BaseWindowSpec {

  private val stockTradeWindow = TradeWindow.forStockTrade()
  private val earlier          = OffsetDateTime.now()
  private val later            = earlier.plusSeconds(1)

  "TradeWindow" should "correctly calculate stock quotation window" in {
    val stockTrade1 = StockTrade(
      tradeId = 1L,
      symbol  = "AAPL",
      settle = StockTradeUnit(
        exchange = "X",
        price    = Money(100.0, "USD"),
        lotSize  = 15L
      ),
      marketTimestamp = earlier,
      ingestTimestamp = earlier,
      conditions      = List(),
      tape            = "X"
    )

    val stockTrade2 = StockTrade(
      tradeId = 2L,
      symbol  = "AAPL",
      settle = StockTradeUnit(
        exchange = "X",
        price    = Money(90.0, "USD"),
        lotSize  = 10L
      ),
      marketTimestamp = later,
      ingestTimestamp = later,
      conditions      = List(),
      tape            = "X"
    )

    val window    = new TimeWindow(0, 1000)
    val collector = new ListCollector[WindowedTrades]

    stockTradeWindow.apply("AAPL", window, List(stockTrade1, stockTrade2), collector)

    val result: List[WindowedTrades] = collector.getResults

    result.size should be(1)
    result.head.symbol should be("AAPL")
    result.head.recordCount should be(2L)
    result.head.priceAtWindowStart should be(BigDecimal(100.0))
    result.head.minPrice should be(BigDecimal(90.0))
    result.head.maxPrice should be(BigDecimal(100.0))
    result.head.priceAtWindowEnd should be(BigDecimal(90.0))
    result.head.sumQuantity should be(BigDecimal(25))
    result.head.sumNotional should be(BigDecimal(2400.0))
    result.head.volumeWeightedAvgPrice should be(BigDecimal(96.0))
  }
}
