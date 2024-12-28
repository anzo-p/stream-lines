package net.anzop.processors.Trend

import breeze.linalg.DenseVector
import net.anzop.models.MarketData
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.util.Collector

class TrendProcessor(trendDiscoverer: TrendDiscoverer)
    extends RichFlatMapFunction[List[MarketData], List[TrendSegment]] {

  override def flatMap(chunk: List[MarketData], out: Collector[List[TrendSegment]]): Unit =
    trendDiscoverer.processChunk(DenseVector(chunk.toArray)) match {
      case List() =>
      case trend  => out.collect(trend)
    }
}
