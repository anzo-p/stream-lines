package net.anzop.processors.Trend

import breeze.linalg.DenseVector
import net.anzop.helpers.DateAndTimeHelpers.oneWeekInMillis
import net.anzop.models.MarketData
import net.anzop.models.Types.DV
import net.anzop.processors.AutoResettingProcessor
import org.apache.flink.api.common.functions.RichFlatMapFunction
import org.apache.flink.api.common.state.{MapState, MapStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.util.Collector
import org.slf4j.Logger

import scala.collection.compat.toTraversableLikeExtensionMethods
import scala.jdk.CollectionConverters.iterableAsScalaIterableConverter

class TrendProcessor(config: TrendConfig, trendDiscoverer: TrendDiscoverer)
    extends RichFlatMapFunction[List[MarketData], List[TrendSegment]]
    with AutoResettingProcessor {

  private val logger: Logger = org.slf4j.LoggerFactory.getLogger(getClass)

  @transient private var trendState: MapState[Long, Array[MarketData]] = _

  private def findPrevBatchRemains(newHeadTs: Long): Option[(Long, Array[MarketData])] =
    trendState
      .entries()
      .asScala
      .filter { entry =>
        // expecting max one, and exactly one banking day before
        newHeadTs - entry.getKey <= oneWeekInMillis
      }
      .toSeq
      .sortBy(_.getKey)
      .headOption
      .map { entry =>
        entry.getKey -> entry.getValue
      }

  private def updateState(dv: DV[MarketData]): Unit =
    if (dv.length > 0) {
      val chunk = dv.toArray
      trendState.put(chunk.last.timestamp, chunk)
    }

  override val earliestExpectedElemTimestamp: Long = config.earliestHistoricalDate.toEpochMilli

  override def resetOp: () => Unit = trendState.clear

  override def open(parameters: Configuration): Unit =
    trendState = getRuntimeContext.getMapState(
      new MapStateDescriptor[Long, Array[MarketData]](
        "trendState",
        classOf[Long],
        classOf[Array[MarketData]]
      )
    )

  override def flatMap(chunk: List[MarketData], out: Collector[List[TrendSegment]]): Unit = {
    val newChunk = chunk.toArray

    newChunk
      .headOption
      .foreach(elem =>
        if (autoResetState(elem.timestamp)) {
          logger.info(s"Trend - Data indicates reset; clearing state")
        })

    val data: DV[MarketData] = findPrevBatchRemains(newChunk(0).timestamp) match {
      case Some((entryKey, remains)) =>
        trendState.remove(entryKey)
        DenseVector(
          (remains ++ newChunk)
            .distinctBy(_.timestamp)
            .sortBy(_.timestamp)
        )
      case None =>
        DenseVector(newChunk)
    }

    val TrendDiscovery(discovered, tail, tailData) = trendDiscoverer.processChunk(data)

    out.collect {
      tail match {
        case Some(tail) => discovered :+ tail
        case None       => discovered
      }
    }

    updateState(tailData)
  }
}
