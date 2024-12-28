package net.anzop.helpers

import breeze.linalg._
import net.anzop.models.Types.DV

import scala.reflect.ClassTag

case class LinearRegression(slope: Double, intercept: Double, variance: Double)

object StatisticsHelpers {

  def linearRegression(dataSet: DV[Double]): LinearRegression = {
    val normalizedData = Analytical.asPercentageChange(dataSet)

    // Create x-values (0 to dataSet.length - 1)
    val x = DenseVector.rangeD(0, normalizedData.length)

    // Design matrix [x, 1] for linear regression
    val X = DenseMatrix.horzcat(x.asDenseMatrix.t, DenseVector.ones[Double](x.length).asDenseMatrix.t)

    // Solve for coefficients (slope and intercept): beta = (X^T X)^-1 X^T y
    val coefficients = inv(X.t * X) * X.t * normalizedData

    // Residuals: y - X * beta
    val residuals = normalizedData - (X * coefficients)

    // Variance: mean squared residuals
    val variance = residuals.map(r => r * r).reduce(_ + _) / residuals.length

    LinearRegression(coefficients(0), coefficients(1), variance)
  }

  def maxDeviationPoint[T : ClassTag](
      deviatingTrend: DV[T],
      overallTrend: DV[T],
      alpha: Double, // 0.2 // 0 tends to edges - 1.0 biases towards the center
      getMetadata: T => Long,
      getValue: T => Double
    ): (T, Long, Int) = {
    require(
      deviatingTrend.length <= overallTrend.length,
      "Deviating trend must be a subset of the overall trend."
    )

    val center = deviatingTrend.length / 2
    val residuals: Array[(T, Double, Int)] = deviatingTrend
      .toArray
      .zip(overallTrend(0 until deviatingTrend.length).toArray)
      .zipWithIndex
      .map {
        case ((deviatingPoint, overallPoint), index) =>
          val rawResidual        = Math.abs(getValue(deviatingPoint) - getValue(overallPoint))
          val normalizedResidual = rawResidual / (1 + Math.abs(index - center))
          val finalResidual      = alpha * normalizedResidual + (1 - alpha) * rawResidual

          (deviatingPoint, finalResidual, index)
      }

    // Find the point with the highest combined residual
    val (maxPoint, _, maxIndex) = residuals.maxBy { case (_, residual, _) => residual }
    (maxPoint, getMetadata(maxPoint), maxIndex)
  }

  def tippingPoint[T : ClassTag](
      overallTrend: DV[T],
      deviatingTrend: DV[T],
      tolerance: Double,
      getMetadata: T => Long,
      getValue: T => Double
    ): Option[(T, Long, Int)] = {
    require(
      deviatingTrend.length <= overallTrend.length &&
        deviatingTrend.toArray.last == overallTrend.toArray.last,
      "Deviating trend must be a subset of the overall trend."
    )

    val residuals: Array[(T, Double, Int)] = deviatingTrend
      .toArray
      .zip(overallTrend(0 until deviatingTrend.length).toArray)
      .zipWithIndex
      .map {
        case ((deviatingPoint, overallPoint), index) =>
          val residual = Math.abs(getValue(deviatingPoint) - getValue(overallPoint))
          (deviatingPoint, residual, index)
      }

    // Find the last point within tolerance
    val tippingPoint = Predef
      .refArrayOps(residuals.reverse)
      .find { case (_, residual, _) => residual <= tolerance }
      .map { case (point, _, index) => (point, getMetadata(point), index) }

    require(
      tippingPoint.isEmpty || deviatingTrend.toArray.map(getMetadata).contains(tippingPoint.map(_._2).get),
      s"tippingPoint not in deviatingTrend"
    )
    tippingPoint
  }
}
