package net.anzop.processors.Trend

import breeze.linalg.DenseVector
import net.anzop.helpers.ArrayHelpers.appendFromHead
import net.anzop.helpers.LinearRegression
import net.anzop.helpers.StatisticsHelpers.{linearRegression, tippingPoint}
import net.anzop.models.MarketData
import net.anzop.models.Types.DV
import net.anzop.processors.Trend.models.{TrendDiscovery, TrendSegment}
import org.slf4j.Logger

import java.time.Instant
import java.time.temporal.ChronoUnit
import scala.annotation.tailrec

class TrendDiscoverer(trendConfig: TrendConfig) extends Serializable {
  final private case class TrendSegmentConfirmation(segment: TrendSegment, tippingElementAtTailSegment: Int)

  private val logger: Logger = org.slf4j.LoggerFactory.getLogger(getClass)

  private val minimumSegmentAndTail: Int = 2 * trendConfig.minimumWindow

  private def isInsufficient(data: DV[MarketData]): Boolean =
    data.length < minimumSegmentAndTail

  private def isTooRecent(dataChunk: DV[MarketData]): Boolean = {
    val tooRecent = Instant
      .now()
      .minus(trendConfig.minimumWindow.toLong, ChronoUnit.DAYS)
      .toEpochMilli

    dataChunk.data.last.timestamp >= tooRecent
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
      getValue       = _.priceChangeAvg
    ).map { case (_, time, index) => (time, index) }
      .getOrElse((window(window.length - tailSegment.length).timestamp, 0))

    val trendSegment = TrendSegment.make(
      begins           = window(0).timestamp,
      ends             = tippingTime,
      linearRegression = overallTrend
    )

    TrendSegmentConfirmation(trendSegment, tippingIndex)
  }

  private def discover(
      window: DV[MarketData],
      tailSegment: DV[MarketData],
      remainingData: DV[MarketData]
    ): (Option[TrendSegment], DV[MarketData], DV[MarketData]) = {
    val overallTrend = linearRegression(window.map(_.priceChangeAvg))
    val tailTrend    = linearRegression(tailSegment.map(_.priceChangeAvg))
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
  private def runDiscoveryLoop(
      currentWindow: DV[MarketData],
      remainingData: DV[MarketData],
      discoveredTrend: List[TrendSegment]
    ): TrendDiscovery = {
    if (isInsufficient(remainingData)) {
      logger.warn(
        s"Trend - recur - Insufficient remaining data (${remainingData.length}) to continue. " +
          s"Current window: ${currentWindow.length}, discovered trends: ${discoveredTrend.length}")

      val entireTail = DenseVector.vertcat(currentWindow, remainingData)

      if (isTooRecent(remainingData)) {
        logger.warn(s"Trend - recur - Appears like the true tail, make temporary trend and exit")
        TrendDiscovery(
          discovered    = discoveredTrend,
          undecidedTail = Some(entireTail).filter(isTooRecent).map(TrendSegment.makeTail),
          tailData      = entireTail
        )
      }
      else {
        logger.warn(s"Trend - recur - This is not yet the true tail, store found trends")
        TrendDiscovery(
          discovered    = discoveredTrend,
          undecidedTail = None,
          tailData      = entireTail
        )
      }
    }
    else {
      val (decRemaining, incWindow) = appendFromHead(remainingData, currentWindow, 1)
      val taiSegmentStart           = incWindow.length - trendConfig.minimumWindow
      val tailSegment               = incWindow(taiSegmentStart until incWindow.length)

      val (maybeTrendSegment, updatedWindow, updatedRemaining) = discover(incWindow, tailSegment, decRemaining)

      val updatedTrend = maybeTrendSegment match {
        case Some(trendSegment) => discoveredTrend :+ trendSegment
        case None               => discoveredTrend
      }

      runDiscoveryLoop(updatedWindow, updatedRemaining, updatedTrend)
    }
  }

  def processChunk(dataChunk: DV[MarketData]): TrendDiscovery = {
    if (isInsufficient(dataChunk)) {
      logger.warn(s"Trend - Insufficient data (${dataChunk.length}) to discover trends")

      if (isTooRecent(dataChunk)) {
        logger.warn(s"Trend - Make open tail")
        TrendDiscovery(
          discovered    = Nil,
          undecidedTail = Some(TrendSegment.makeTail(dataChunk)),
          tailData      = dataChunk
        )
      }
      else
        logger.warn(s"Trend - Just exit")
      TrendDiscovery.boomerang(dataChunk)
    }
    else {
      runDiscoveryLoop(
        currentWindow   = dataChunk(0 until minimumSegmentAndTail - 1),
        remainingData   = dataChunk(minimumSegmentAndTail until dataChunk.length),
        discoveredTrend = List()
      )
    }
  }
}
