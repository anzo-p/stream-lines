package net.anzop.helpers

import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class MonetarySpec extends AnyFlatSpec with Matchers {

  "toDecimal" should "convert to decimal" in {
    Monetary.toDecimal(0, 0) should be(Right(0.0))
    Monetary.toDecimal(0, 1) should be(Right(0.000000001))
    Monetary.toDecimal(0, 1 * 100 * 1000 * 1000) should be(Right(0.1))

    Monetary.toDecimal(1, 0) should be(Right(1.0))
    Monetary.toDecimal(1, 1) should be(Right(1.000000001))
    Monetary.toDecimal(1, 99 * 10 * 1000 * 1000) should be(Right(1.99))
    Monetary.toDecimal(1, 1000000000) should be(Right(2.0))

    Monetary.toDecimal(0, -1) should be(Right(-0.000000001))
    Monetary.toDecimal(0, -1 * 100 * 1000 * 1000) should be(Right(-0.1))

    Monetary.toDecimal(-1, 0) should be(Right(-1.0))
    Monetary.toDecimal(-1, -1) should be(Right(-1.000000001))
    Monetary.toDecimal(-1, -99 * 10 * 1000 * 1000) should be(Right(-1.99))
    Monetary.toDecimal(-1, -1000000000) should be(Right(-2.0))
  }

  "toDecimal" should "fail when units and nanos have different sign" in {
    Monetary.toDecimal(-1, 1) should be(Left("Units and nanos must have the same sign"))
    Monetary.toDecimal(1, -1) should be(Left("Units and nanos must have the same sign"))
  }
}
