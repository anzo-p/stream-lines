package net.anzop

import net.anzop.config.{InfluxConfig, SourceRunnerConfig, StreamConfig}
import net.anzop.models.MarketData
import net.anzop.processors.Drawdown.{Drawdown, DrawdownConfig, DrawdownProcessor}
import net.anzop.processors.Trend.{TrendConfig, TrendDiscoverer, TrendProcessor, TrendSegment}
import net.anzop.sinks.influxdb.InfluxHttpSink
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
      env.addSource(new MarketDataSource(influxDetails, SourceRunnerConfig.values))

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

    val flattenedTrendStream: DataStream[TrendSegment] =
      trendStream.flatMap(_.iterator)

    flattenedTrendStream.addSink(new InfluxHttpSink[TrendSegment](influxDetails))

    val drawDownStream: DataStream[Drawdown] =
      dataStream
        .keyBy(_.field)
        .process(new DrawdownProcessor(DrawdownConfig.values))

    drawDownStream.addSink(new InfluxHttpSink[Drawdown](influxDetails))

    env.execute("InfluxDB Source Example")
  }
}
