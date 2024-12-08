package net.anzop.helpers

import breeze.linalg.DenseVector

import scala.language.higherKinds

object Analytical {

  def asPercentageChange(dataSet: DenseVector[Double]): DenseVector[Double] =
    dataSet.map(value => ((value / dataSet(0)) - 1) * 100)
}
