package net.anzop

import net.anzop.config.{InfluxDetails, TrendConfig}
import net.anzop.models.{DrawdownData, MarketData, TrendSegment}
import net.anzop.processors.Drawdown.Drawdown
import net.anzop.processors.RegressionTrend.{TrendDiscoverer, TrendProcessor}
import net.anzop.sources.IndexDataSource
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows
import org.apache.flink.streaming.api.windowing.triggers.CountTrigger
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow
import org.apache.flink.util.Collector

object Currents {

  def main(args: Array[String]): Unit = {
    val trendConfig = TrendConfig.values
    val env         = StreamExecutionEnvironment.getExecutionEnvironment

    env
      .getConfig
      .setGlobalJobParameters(new ExecutionConfig.GlobalJobParameters {
        override def toMap: java.util.Map[String, String] = {
          java.util.Collections.singletonMap("log.level", "INFO")
        }
      })

    val dataStream: DataStream[MarketData] =
      env.addSource(new IndexDataSource(InfluxDetails.make()))

    val batchedStream: DataStream[List[MarketData]] =
      dataStream
        .windowAll(GlobalWindows.create())
        .trigger(CountTrigger.of(trendConfig.flinkWindowCount))
        .apply((_: GlobalWindow, elements: Iterable[MarketData], out: Collector[List[MarketData]]) => {
          out.collect(elements.toList)
        })

    val trendStream: DataStream[List[TrendSegment]] =
      batchedStream
        .keyBy(_.head.field)
        .flatMap(new TrendProcessor(new TrendDiscoverer(trendConfig)))

    trendStream.print()

    val drawDownStream: DataStream[DrawdownData] =
      dataStream
        .keyBy(_.field)
        .process(new Drawdown())

    drawDownStream.print()

    env.execute("InfluxDB Source Example")
  }
}
