package net.anzop.processors

import breeze.linalg.DenseVector
import net.anzop.config.TrendDiscoveryConfig
import net.anzop.helpers.ArrayHelpers
import net.anzop.helpers.StatisticsHelpers.{linearRegression, tippingPoint}
import net.anzop.models.Types.DV
import net.anzop.models.{MarketData, TrendSegment}

import scala.annotation.tailrec
import scala.reflect.ClassTag

class TrendDiscoverer(trendConfig: TrendDiscoveryConfig) extends Serializable {

  private def appendFromHead[T : ClassTag](
      dest: DV[T],
      src: DV[T],
      n: Int,
      fixedSizeDest: Boolean = false
    ): (DV[T], DV[T]) = {
    val (newDest, newSrc) = ArrayHelpers.appendFromHead(dest.toArray, src.toArray, n)
    val fixedDest         = if (fixedSizeDest) newDest.drop(n) else newDest
    (DenseVector(fixedDest), DenseVector(newSrc))
  }

  private def discoverTrendSegment(
      window: DV[MarketData],
      tailSegment: DV[MarketData],
      remainingData: DV[MarketData]
    ): (Option[TrendSegment], DV[MarketData], DV[MarketData]) = {
    val overallTrend = linearRegression(window.map(_.value))
    val tailTrend    = linearRegression(tailSegment.map(_.value))
    val slopeDiff    = Math.abs(overallTrend.slope - tailTrend.slope)
    //val varianceDiff = Math.abs(overallTrend.variance - tailTrend.variance)

    if (slopeDiff > trendConfig.regressionSlopeThreshold) { //} || varianceDiff > trendConfig.regressionVarianceThreshold) {
      val trendConfirmation = tippingPoint[MarketData](
        deviatingTrend = tailSegment,
        overallTrend   = window,
        tolerance      = trendConfig.tippingPointThreshold,
        getMetadata    = _.timestamp,
        getValue       = _.value
      )

      val (tippingTime, tippingIndex) = trendConfirmation
        .map { case (_, time, index) => (time, index) }
        .getOrElse((window(window.length - tailSegment.length).timestamp, 0))

      val trendSegment = TrendSegment.make(
        begins               = window(0).timestamp,
        ends                 = tippingTime,
        trendAngleAnnualized = overallTrend.slope * 365, // annualized
        linearRegression     = overallTrend
      )

      println(s"Discovered new $trendSegment")

      val (newTailSegment, newRemainingData) =
        appendFromHead(tailSegment, remainingData, tippingIndex, fixedSizeDest = true)

      (Some(trendSegment), newTailSegment, newRemainingData)
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
      val (incWindow, decRemaining) = appendFromHead(currentWindow, remainingData, 1)
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
      val initialWindow = dataChunk(0 until trendConfig.minimumWindow - 1)
      val remainingData = dataChunk(trendConfig.minimumWindow until dataChunk.length)
      processChunk(initialWindow, remainingData, List.empty)
    }
}
