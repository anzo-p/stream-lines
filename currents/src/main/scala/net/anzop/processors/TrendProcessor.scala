package net.anzop.processors

import breeze.linalg.DenseVector
import net.anzop.models.{MarketData, TrendSegment}
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{ListState, ListStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.util.Collector

import scala.jdk.CollectionConverters.asScalaIteratorConverter

class TrendProcessor(trendDiscoverer: TrendDiscoverer)
    extends RichFlatMapFunction[List[MarketData], List[TrendSegment]] {

  @transient private var trendState: ListState[DenseVector[MarketData]] = _

  private def updateState(lastSegment: DenseVector[MarketData]): Unit = {
    trendState.clear()
    trendState.add(lastSegment)
  }

  override def flatMap(chunk: List[MarketData], out: Collector[List[TrendSegment]]): Unit = {
    val remainder: Option[DenseVector[MarketData]] = trendState.get().iterator().asScala.toList.headOption
    val newData: DenseVector[MarketData]           = DenseVector(chunk.toArray)

    val dataChunk = remainder match {
      case Some(prevData) => DenseVector.vertcat(prevData, newData)
      case None           => newData
    }

    val (trend, remains): (List[TrendSegment], DenseVector[MarketData]) = trendDiscoverer.processChunk(dataChunk)

    out.collect(trend)
    updateState(remains)
  }

  override def open(parameters: Configuration): Unit = {
    val descriptor =
      new ListStateDescriptor[DenseVector[MarketData]]("lastTrendState", classOf[DenseVector[MarketData]])
    trendState = getRuntimeContext.getListState(descriptor)
  }
}
