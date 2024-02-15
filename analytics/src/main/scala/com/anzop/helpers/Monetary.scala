package com.anzop.helpers

import java.math.{MathContext, RoundingMode}

object Monetary {

  def toDecimal(units: Long, nanos: Int): Either[String, BigDecimal] = {
    if (units.signum != 0 && nanos.signum != 0 && units.signum != nanos.signum) {
      Left("Units and nanos must have the same sign")
    }
    else {
      val unitsPart      = BigDecimal(units)
      val preciseDivisor = BigDecimal("1000000000")
      val nanosPart      = BigDecimal(nanos) / preciseDivisor(new MathContext(10, RoundingMode.HALF_UP))
      Right(unitsPart + nanosPart)
    }
  }
}
