package net.anzop.processors.Trend.models

import net.anzop.helpers.StatisticsHelpers.linearRegression
import net.anzop.helpers.{DateAndTimeHelpers, LinearRegression}
import net.anzop.models.MarketData
import net.anzop.models.Types.DV
import net.anzop.sinks.influxdb.InfluxSerializable

import java.time.{Duration, Instant}

case class TrendSegment(
    timestamp: Long,
    established: Boolean,
    begins: Long,
    ends: Long,
    growth: Double,
    regressionSlope: Double,
    regressionIntercept: Double,
    regressionVariance: Double
  ) extends InfluxSerializable {

  override val measurement = "trends-by-statistical-regression"

  override def fields: Map[String, Any] =
    Map(
      "begins"               -> begins,
      "ends"                 -> ends,
      "growth"               -> growth,
      "regression_slope"     -> regressionSlope,
      "regression_intercept" -> regressionIntercept,
      "regression_variance"  -> regressionVariance
    )

  override def tags: Map[String, String] =
    Map("type" -> "segment", "established" -> established.toString)

  override def toString: String =
    s"""TrendSegment(
       |begins: ${DateAndTimeHelpers.epochToStringDate(begins)},
       |ends: ${DateAndTimeHelpers.epochToStringDate(ends)},
       |growth: $growth,
       |regressionSlope: $regressionSlope,
       |regressionIntercept: $regressionIntercept,
       |regressionVariance: $regressionVariance)
       |""".stripMargin
}

object TrendSegment {

  def make(
      begins: Long,
      ends: Long,
      linearRegression: LinearRegression,
      established: Boolean = true
    ): TrendSegment = {
    val days = Duration
      .between(
        Instant.ofEpochMilli(begins),
        Instant.ofEpochMilli(ends)
      )
      .toDays

    TrendSegment(
      timestamp           = ends,
      established         = established,
      begins              = begins,
      ends                = ends,
      growth              = linearRegression.slope * days,
      regressionSlope     = linearRegression.slope,
      regressionIntercept = linearRegression.intercept,
      regressionVariance  = linearRegression.variance
    )
  }

  def makeTail(data: DV[MarketData]): TrendSegment =
    make(
      established      = false,
      begins           = data(0).timestamp,
      ends             = data(data.length - 1).timestamp,
      linearRegression = linearRegression(data.map(_.priceChangeAvg))
    )

}
