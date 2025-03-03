package net.anzop.processors.Trend

import net.anzop.helpers.{DateAndTimeHelpers, LinearRegression}
import net.anzop.sinks.influxdb.InfluxSerializable

case class TrendSegment(
    timestamp: Long,
    begins: Long,
    ends: Long,
    growth: Double,
    regressionSlope: Double,
    regressionIntercept: Double,
    regressionVariance: Double
  ) extends InfluxSerializable {

  override val measurement = "trend"

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
    Map("type" -> "segment")

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
      growth: Double,
      linearRegression: LinearRegression
    ): TrendSegment =
    TrendSegment(
      timestamp           = ends, // a trend (segment) is established at its ending date as a new one begins
      begins              = begins,
      ends                = ends,
      growth              = growth,
      regressionSlope     = linearRegression.slope,
      regressionIntercept = linearRegression.intercept,
      regressionVariance  = linearRegression.variance
    )
}
