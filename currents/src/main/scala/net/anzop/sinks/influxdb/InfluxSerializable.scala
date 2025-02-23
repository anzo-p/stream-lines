package net.anzop.sinks.influxdb

import com.influxdb.client.domain.WritePrecision
import com.influxdb.client.write.Point

trait InfluxSerializable {
  def measurement: String
  def timestamp: Long
  def fields: Map[String, Any]
  def tags: Map[String, String]
  def toPoint: Point = InfluxSerializable.toPoint(this)
  /* @formatter:off */
  def toLineProtocol(point: Point): String = point.toLineProtocol
  /* @formatter:on */
  def toLineProtocol: String = toPoint.toLineProtocol
}

object InfluxSerializable {

  private def setScale(v: BigDecimal): BigDecimal =
    v.setScale(10, BigDecimal.RoundingMode.HALF_UP)

  def toPoint(obj: InfluxSerializable): Point = {
    val basePoint = Point
      .measurement(obj.measurement)
      .time(obj.timestamp * 1000000L, WritePrecision.NS)

    obj.tags.foldLeft(basePoint) { case (p, (k, v)) => p.addTag(k, v) }

    obj.fields.foldLeft(basePoint) {
      case (p, (k, v)) =>
        v match {
          case v: BigDecimal => p.addField(k, setScale(v).doubleValue)
          case v: Boolean    => p.addField(k, v)
          case v: Double     => p.addField(k, v)
          case v: Int        => p.addField(k, v)
          case v: Long       => p.addField(k, v)
          case v: String     => p.addField(k, v)
          case _             => throw new IllegalArgumentException(s"Unsupported field type for key '$k': ${v.getClass}")
        }
    }
  }
}
