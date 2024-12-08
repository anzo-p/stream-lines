package net.anzop.helpers

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateHelpers {

  def epochToStringDate(value: Long, zoneId: String = "UTC"): String = {
    val instant   = Instant.ofEpochMilli(value)
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of(zoneId))
    formatter.format(instant)
  }
}
