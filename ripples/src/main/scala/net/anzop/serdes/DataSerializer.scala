package net.anzop.serdes

import java.time.OffsetDateTime

trait DataSerializer[T] {
  def serialize(data: T): String

  protected def setScale(v: BigDecimal): BigDecimal =
    v.setScale(10, BigDecimal.RoundingMode.HALF_UP)

  protected def setScale(v: BigInt): BigDecimal =
    setScale(v.toDouble)

  protected def serializeTags(tags: Map[String, String]): String =
    tags.map { case (k, v) => s"$k=$v" }.mkString(",")

  protected def dateTimeToLong(windowEndTime: OffsetDateTime): Long =
    windowEndTime.toInstant.getEpochSecond * 1000000000L
}
