package net.anzop.helpers

import net.anzop.models.Types.DV

import scala.language.higherKinds

object Analytical {

  def asPercentageChange(dataSet: DV[Double]): DV[Double] =
    dataSet.map(value => ((value / dataSet(0)) - 1) * 100)
}
