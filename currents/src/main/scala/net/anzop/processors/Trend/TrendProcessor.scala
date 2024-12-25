package net.anzop.processors.Trend

import breeze.linalg.DenseVector
import net.anzop.models.MarketData
import net.anzop.models.Types.DV
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.util.Collector

import scala.jdk.CollectionConverters.asScalaIteratorConverter

class TrendProcessor(trendDiscoverer: TrendDiscoverer)
    extends RichFlatMapFunction[List[MarketData], List[TrendSegment]] {

  @transient private var trendState: ListState[DV[MarketData]] = _

  private def updateState(lastSegment: DV[MarketData]): Unit = {
    trendState.clear()
    trendState.add(lastSegment)
  }

  override def flatMap(chunk: List[MarketData], out: Collector[List[TrendSegment]]): Unit = {
    val remainder: Option[DV[MarketData]] = trendState.get().iterator().asScala.toList.headOption
    val newData: DV[MarketData]           = DenseVector(chunk.toArray)

    val dataChunk = remainder match {
      case Some(prevData) => DenseVector.vertcat(prevData, newData)
      case None           => newData
    }

    val (trend, remains): (List[TrendSegment], DV[MarketData]) = trendDiscoverer.processChunk(dataChunk)

    out.collect(trend)
    updateState(remains)
  }

  override def open(parameters: Configuration): Unit = {
    val descriptor =
      new ListStateDescriptor[DV[MarketData]]("lastTrendState", classOf[DV[MarketData]])
    trendState = getRuntimeContext.getListState(descriptor)
  }
}
