package net.anzop.processors.Trend

import breeze.linalg.DenseVector
import net.anzop.config.TrendConfig
import net.anzop.helpers.ArrayHelpers.appendFromHead
import net.anzop.helpers.LinearRegression
import net.anzop.helpers.StatisticsHelpers.{linearRegression, tippingPoint}
import net.anzop.models.MarketData
import net.anzop.models.Types.DV

import java.time.{Duration, Instant}
import scala.annotation.tailrec

class TrendDiscoverer(trendConfig: TrendConfig) extends Serializable {
  final private case class TrendSegmentConfirmation(segment: TrendSegment, tippingElementAtTailSegment: Int)

  private val minimumSegmentAndTail: Int = 2 * trendConfig.minimumWindow

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

    if (slopeDiff > trendConfig.regressionSlopeThreshold) {
      val trendSegment = createSegment(window, tailSegment, overallTrend)
      val (newRemainingData, newTailSegment) =
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
    if (remainingData.length < minimumSegmentAndTail) {
      (discoveredTrend, DenseVector.vertcat(currentWindow, remainingData))
    }
    else {
      val (decRemaining, incWindow) = appendFromHead(remainingData, currentWindow, 1)
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
    if (dataChunk.length < minimumSegmentAndTail) {
      (List(), dataChunk)
    }
    else {
      processChunk(
        currentWindow   = dataChunk(0 until trendConfig.minimumWindow - 1),
        remainingData   = dataChunk(trendConfig.minimumWindow until dataChunk.length),
        discoveredTrend = List()
      )
    }
}
