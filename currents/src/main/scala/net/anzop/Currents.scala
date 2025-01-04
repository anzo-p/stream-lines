package net.anzop

import net.anzop.config.{InfluxConfig, StreamConfig, TrendConfig}
import net.anzop.models.MarketData
import net.anzop.processors.Drawdown.{Drawdown, DrawdownProcessor, DrawdownSerDes}
import net.anzop.processors.Trend.{TrendDiscoverer, TrendProcessor, TrendSegment, TrendSegmentSerDes}
import net.anzop.sinks.ResultSink
import net.anzop.sources.marketData.MarketDataSource
import net.anzop.triggers.CountOrTimerTrigger
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.GlobalWindows
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow
import org.apache.flink.util.Collector

object Currents {

  def main(args: Array[String]): Unit = {
    val influxDetails = InfluxConfig.values
    val trendConfig   = TrendConfig.values
    val env           = StreamConfig.createExecutionEnvironment()

    val dataStream: DataStream[MarketData] =
      env.addSource(new MarketDataSource(influxDetails))

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

    trendStream.addSink(new ResultSink(influxDetails, TrendSegmentSerDes(influxDetails.trendMeasure)))

    val drawDownStream: DataStream[Drawdown] =
      dataStream
        .keyBy(_.field)
        .process(new DrawdownProcessor())

    drawDownStream.addSink(new ResultSink(influxDetails, DrawdownSerDes(influxDetails.drawdownMeasure)))

    env.execute("InfluxDB Source Example")
  }
}
