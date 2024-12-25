package net.anzop.processors.Trend

import breeze.linalg.DenseVector
import net.anzop.config.TrendConfig
import net.anzop.helpers.StatisticsHelpers.{linearRegression, tippingPoint}
import net.anzop.helpers.{ArrayHelpers, LinearRegression}
import net.anzop.models.Types.DV
import net.anzop.models.MarketData

import java.time.{Duration, Instant}
import scala.annotation.tailrec
import scala.reflect.ClassTag

class TrendDiscoverer(trendConfig: TrendConfig) extends Serializable {
  final private case class TrendSegmentConfirmation(segment: TrendSegment, tippingElementAtTailSegment: Int)

  private def appendFromHead[T : ClassTag](
      src: DV[T],
      dest: DV[T],
      n: Int,
      fixedSizeDest: Boolean = false
    ): (DV[T], DV[T]) = {
    val (newDest, newSrc) = ArrayHelpers.appendFromHead(dest.toArray, src.toArray, n)
    val fixedDest         = if (fixedSizeDest) newDest.drop(n) else newDest
    (DenseVector(fixedDest), DenseVector(newSrc))
  }

  private def createSegment(
      window: DV[MarketData],
      tailSegment: DV[MarketData],
      overallTrend: LinearRegression
    ): TrendSegmentConfirmation = {
    val (tippingTime, tippingIndex) = tippingPoint[MarketData](
      overallTrend   = window,
      deviatingTrend = tailSegment,
      tolerance      = trendConfig.tippingPointThreshold,
      getMetadata    = _.timestamp,
      getValue       = _.value
    ).map { case (_, time, index) => (time, index) }
      .getOrElse((window(window.length - tailSegment.length).timestamp, 0))

    val days = Duration
      .between(
        Instant.ofEpochMilli(window(0).timestamp),
        Instant.ofEpochMilli(tippingTime)
      )
      .toDays

    val trendSegment = TrendSegment.make(
      begins           = window(0).timestamp,
      ends             = tippingTime,
      growth           = overallTrend.slope * days,
      linearRegression = overallTrend
    )

    TrendSegmentConfirmation(trendSegment, tippingIndex)
  }

  private def discoverTrendSegment(
      window: DV[MarketData],
      tailSegment: DV[MarketData],
      remainingData: DV[MarketData]
    ): (Option[TrendSegment], DV[MarketData], DV[MarketData]) = {
    val overallTrend = linearRegression(window.map(_.value))
    val tailTrend    = linearRegression(tailSegment.map(_.value))
    val slopeDiff    = Math.abs(overallTrend.slope - tailTrend.slope)
    val varianceDiff = Math.abs(overallTrend.variance - tailTrend.variance)

    if (slopeDiff > trendConfig.regressionSlopeThreshold &&
        (overallTrend.slope < 0 || varianceDiff < trendConfig.regressionVarianceLimit)) {

      val trendSegment = createSegment(window, tailSegment, overallTrend)
      val (newTailSegment, newRemainingData) =
        appendFromHead(
          src           = remainingData,
          dest          = tailSegment,
          n             = trendSegment.tippingElementAtTailSegment,
          fixedSizeDest = true)

      (Some(trendSegment.segment), newTailSegment, newRemainingData)
    }
    else {
      (None, window, remainingData)
    }
  }

  @tailrec
  private def processChunk(
      currentWindow: DV[MarketData],
      remainingData: DV[MarketData],
      discoveredTrend: List[TrendSegment]
    ): (List[TrendSegment], DV[MarketData]) =
    if (remainingData.length < trendConfig.minimumWindow) {
      (discoveredTrend, remainingData)
    }
    else {
      val (incWindow, decRemaining) = appendFromHead(remainingData, currentWindow, 1)
      val taiSegmentStart           = incWindow.length - trendConfig.minimumWindow
      val tailSegment               = incWindow(taiSegmentStart until incWindow.length)

      val (maybeTrendSegment, updatedWindow, updatedRemaining) =
        discoverTrendSegment(incWindow, tailSegment, decRemaining)

      val updatedTrend = maybeTrendSegment match {
        case Some(trendSegment) => discoveredTrend :+ trendSegment
        case None               => discoveredTrend
      }

      processChunk(updatedWindow, updatedRemaining, updatedTrend)
    }

  def processChunk(dataChunk: DV[MarketData]): (List[TrendSegment], DV[MarketData]) =
    if (dataChunk.length < 2 * trendConfig.minimumWindow) {
      (List.empty, dataChunk)
    }
    else {
      processChunk(
        currentWindow   = dataChunk(0 until trendConfig.minimumWindow - 1),
        remainingData   = dataChunk(trendConfig.minimumWindow until dataChunk.length),
        discoveredTrend = List.empty
      )
    }
}
