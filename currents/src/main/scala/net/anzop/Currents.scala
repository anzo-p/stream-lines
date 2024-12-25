package net.anzop

import net.anzop.config.{InfluxDetails, TrendConfig}
import net.anzop.models.MarketData
import net.anzop.processors.Drawdown.{Drawdown, DrawdownProcessor, DrawdownSerDes}
import net.anzop.processors.Trend.{ListTrendSegmentSerDes, TrendDiscoverer, TrendProcessor, TrendSegment}
import net.anzop.sinks.ResultSink
import net.anzop.sources.IndexDataSource
import net.anzop.triggers.CountOrTimerTrigger
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow
import org.apache.flink.util.Collector

object Currents {

  def main(args: Array[String]): Unit = {
    val influxDetails = InfluxDetails.make()
    val trendConfig   = TrendConfig.values
    val env           = StreamExecutionEnvironment.getExecutionEnvironment

    env
      .getConfig
      .setGlobalJobParameters(new ExecutionConfig.GlobalJobParameters {
        override def toMap: java.util.Map[String, String] = {
          java.util.Collections.singletonMap("log.level", "INFO")
        }
      })

    val dataStream: DataStream[MarketData] =
      env.addSource(new IndexDataSource(influxDetails))

    val batchedStream: DataStream[List[MarketData]] =
      dataStream
        .windowAll(GlobalWindows.create())
        .trigger(CountOrTimerTrigger.of(trendConfig.flinkWindowCount, trendConfig.flinkWindowInterval))
        .apply((_: GlobalWindow, elements: Iterable[MarketData], out: Collector[List[MarketData]]) => {
          val batch = elements.toList
          if (batch.nonEmpty) {
            out.collect(batch)
          }
        })

    val trendStream: DataStream[List[TrendSegment]] =
      batchedStream
        .keyBy(_.head.field)
        .flatMap(new TrendProcessor(new TrendDiscoverer(trendConfig)))

    trendStream.addSink(new ResultSink(influxDetails, ListTrendSegmentSerDes()))

    val drawDownStream: DataStream[Drawdown] =
      dataStream
        .keyBy(_.field)
        .process(new DrawdownProcessor())

    drawDownStream.addSink(new ResultSink(influxDetails, DrawdownSerDes()))

    env.execute("InfluxDB Source Example")
  }
}
