package net.anzop

import net.anzop.config.{InfluxDetails, TrendDiscoveryConfig}
import net.anzop.models.{MarketData, TrendSegment}
import net.anzop.processors.{TrendDiscoverer, TrendProcessor}
import net.anzop.sources.IndexDataSource
import org.apache.flink.api.common.ExecutionConfig
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.apache.flink.streaming.api.functions.source.SourceFunction
import org.apache.flink.streaming.api.scala._

object Currents {

  class TrendSink() extends SinkFunction[List[TrendSegment]] {
    override def invoke(value: List[TrendSegment], context: SinkFunction.Context): Unit = {
      value.foreach { segment =>
        println(
          s"Segment: ${segment.begins} -> ${segment.ends} | Slope: ${segment.regressionSlope} | Variance: ${segment.regressionVariance}")
      }
    }
  }

  def main(args: Array[String]): Unit = {

    val env = StreamExecutionEnvironment.getExecutionEnvironment

    env
      .getConfig
      .setGlobalJobParameters(new ExecutionConfig.GlobalJobParameters {
        override def toMap: java.util.Map[String, String] = {
          java.util.Collections.singletonMap("log.level", "INFO")
        }
      })

    val influxSource: SourceFunction[List[MarketData]] =
      new IndexDataSource(InfluxDetails.make())

    val dataStream: DataStream[List[MarketData]] = env.addSource(influxSource)

    val keyedStream = dataStream.keyBy(point => point.map(_.timestamp).toString)

    val trendStream: DataStream[List[TrendSegment]] = keyedStream.flatMap(
      new TrendProcessor(new TrendDiscoverer(TrendDiscoveryConfig.values))
    )

    trendStream.addSink(new TrendSink())

    env.execute("InfluxDB Source Example")
  }
}
