package net.anzop.processors.Trend

import net.anzop.helpers.{DateHelpers, LinearRegression}

case class TrendSegment(
    begins: Long,
    ends: Long,
    growth: Double,
    regressionSlope: Double,
    regressionIntercept: Double,
    regressionVariance: Double
  ) {

  override def toString: String =
    s"""TrendSegment(
       |begins: ${DateHelpers.epochToStringDate(begins)},
       |ends: ${DateHelpers.epochToStringDate(ends)},
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
    TrendSegment(begins, ends, growth, linearRegression.slope, linearRegression.intercept, linearRegression.variance)
}
