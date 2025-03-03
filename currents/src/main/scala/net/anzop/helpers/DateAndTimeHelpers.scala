package net.anzop.helpers

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateAndTimeHelpers {

  val oneWeekInMillis: Int = 7 * 24 * 60 * 60 * 1000

  def millisToMinutes(value: Long): Long = value / (60 * 1000)

  def epochToStringDate(value: Long, zoneId: String = "UTC"): String = {
    val instant   = Instant.ofEpochMilli(value)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of(zoneId))
    formatter.format(instant)
  }
}
