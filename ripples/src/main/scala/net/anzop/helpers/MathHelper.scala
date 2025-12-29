package net.anzop.helpers

object MathHelper {

  // Median Absolute Deviation
  def mad(xs: Seq[BigDecimal]): BigDecimal = {
    require(xs.nonEmpty, "MAD of empty sequence")

    val m          = MathHelper.median(xs)
    val deviations = xs.map(x => (x - m).abs)

    MathHelper.median(deviations) * BigDecimal("1.4826")
  }

  def median[A](xs: Seq[A])(implicit num: Numeric[A]): BigDecimal = {
    require(xs.nonEmpty, "median of empty sequence")

    val sorted = xs.map(a => BigDecimal(num.toDouble(a))).sorted
    val n      = sorted.size

    if (n % 2 == 1) {
      sorted(n / 2)
    }
    else {
      (sorted(n / 2 - 1) + sorted(n / 2)) / 2
    }
  }
}
