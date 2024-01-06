import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import processors.{QuotationWindow, StreamHelpers, WindowedQuotationVolumes}
import sinks.ResultSink
import types._

import java.time.{Duration => JavaDuration}

object FlinkApp {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    StreamConfig.configExecEnv(env)

    val streamConsumer = StreamConfig.buildConsumer()

    val marketDataStream: DataStream[MarketDataMessage] =
      env.addSource(streamConsumer)

    val stockQuotationStream: DataStream[StockQuotation] =
      StreamHelpers.filterAndMap[StockQuotation](marketDataStream)

    val watermarkedStockQuotationStream: DataStream[StockQuotation] = StreamHelpers.watermarkForBound(
      stockQuotationStream,
      JavaDuration.ofSeconds(65),
      JavaDuration.ofSeconds(30)
    )

    val windowedStockQuotationVolumes: DataStream[WindowedQuotationVolumes] = watermarkedStockQuotationStream
      .keyBy(_.symbol)
      .window(SlidingProcessingTimeWindows.of(Time.seconds(60), Time.seconds(30)))
      .apply(QuotationWindow.forStockQuotation())

    windowedStockQuotationVolumes.addSink(ResultSink.forStockQuotation())

    env.execute("Flink Kinesis Example")
  }
}
