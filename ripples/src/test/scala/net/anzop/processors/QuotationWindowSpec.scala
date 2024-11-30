package net.anzop.processors

import net.anzop.results.WindowedQuotationVolumes
import net.anzop.types.{Money, StockQuotation, StockTradeUnit}
import org.apache.flink.streaming.api.windowing.windows.TimeWindow
import org.apache.flink.util.Collector
import org.scalatest.BeforeAndAfterAll
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import java.time.OffsetDateTime
import scala.collection.mutable.ListBuffer

class ListCollector[T] extends Collector[T] {
  private val list: ListBuffer[T] = ListBuffer.empty[T]

  override def collect(record: T): Unit = {
    list += record
  }

  override def close(): Unit = {}

  def getResults: List[T] = list.toList
}

class QuotationWindowSpec extends AnyFlatSpec with Matchers with BeforeAndAfterAll {

  private val stockQuotationWindow = QuotationWindow.forStockQuotation()
  private val earlier              = OffsetDateTime.now()
  private val later                = earlier.plusSeconds(1)

  "QuotationWindow" should "calculate stock quotation window" in {
    val stockQuotation1 = StockQuotation(
      symbol = "AAPL",
      bid = StockTradeUnit(
        exchange = "X",
        price    = Money(90.0, "USD"),
        lotSize  = 10L
      ),
      ask = StockTradeUnit(
        exchange = "X",
        price    = Money(100.0, "USD"),
        lotSize  = 15L
      ),
      marketTimestamp = earlier,
      ingestTimestamp = earlier,
      conditions      = List(),
      tape            = "X"
    )

    val stockQuotation2 = StockQuotation(
      symbol = "AAPL",
      bid = StockTradeUnit(
        exchange = "X",
        price    = Money(95.0, "USD"),
        lotSize  = 10L
      ),
      ask = StockTradeUnit(
        exchange = "X",
        price    = Money(99.0, "USD"),
        lotSize  = 15L
      ),
      marketTimestamp = later,
      ingestTimestamp = later,
      conditions      = List(),
      tape            = "X"
    )

    val window    = new TimeWindow(0, 1000)
    val collector = new ListCollector[WindowedQuotationVolumes]

    stockQuotationWindow.apply("AAPL", window, List(stockQuotation1, stockQuotation2), collector)

    val result: List[WindowedQuotationVolumes] = collector.getResults

    result.size should be(1)
    result.head.symbol should be("AAPL")
    result.head.sumBidVolume should be(1850.0)
    result.head.sumAskVolume should be(2985.0)
    result.head.recordCount should be(2)
    result.head.averageBidPrice should be(92.5)
    result.head.averageAskPrice should be(99.5)
    result.head.bidPriceAtWindowEnd should be(95.0)
    result.head.askPriceAtWindowEnd should be(99.0)
  }
}
