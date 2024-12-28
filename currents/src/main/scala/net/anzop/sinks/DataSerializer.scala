package net.anzop.sinks

trait DataSerializer[T] {
  def measurementName: String
  def serialize(data: T): String

  def setScale(v: BigDecimal): BigDecimal =
    v.setScale(10, BigDecimal.RoundingMode.HALF_UP)
}
