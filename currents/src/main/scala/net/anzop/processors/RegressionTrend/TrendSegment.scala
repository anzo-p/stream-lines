package net.anzop.processors.RegressionTrend

import net.anzop.helpers.{DateHelpers, LinearRegression}

case class TrendSegment(
    begins: Long,
    ends: Long,
    trendAngleAnnualized: Double,
    regressionSlope: Double,
    regressionIntercept: Double,
    regressionVariance: Double
  ) {

  override def toString: String =
    s"""TrendSegment(
       |begins: ${DateHelpers.epochToStringDate(begins)},
       |ends: ${DateHelpers.epochToStringDate(ends)},
       |trendAngleAnnualized: $trendAngleAnnualized,
       |regressionSlope: $regressionSlope,
       |regressionIntercept: $regressionIntercept,
       |regressionVariance: $regressionVariance)
       |""".stripMargin
}

object TrendSegment {

  def make(
      begins: Long,
      ends: Long,
      trendAngleAnnualized: Double,
      linearRegression: LinearRegression
    ): TrendSegment =
    TrendSegment(
      begins,
      ends,
      trendAngleAnnualized,
      linearRegression.slope,
      linearRegression.intercept,
      linearRegression.variance)
}
