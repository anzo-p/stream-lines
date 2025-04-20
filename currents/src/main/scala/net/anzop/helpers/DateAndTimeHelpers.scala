package net.anzop.helpers

import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.time.{Instant, LocalDateTime, ZoneId}

object DateAndTimeHelpers {

  val oneWeekInMillis: Int = 7 * 24 * 60 * 60 * 1000

  def millisToMinutes(value: Long): Long = value / (60 * 1000)

  def epochToStringDate(value: Long, zoneId: String = "UTC"): String =
    DateTimeFormatter
      .ofPattern("yyyy-MM-dd HH:mm:ss")
      .withZone(ZoneId.of(zoneId))
      .format(Instant.ofEpochMilli(value))

  def isBeforeToday(ts: Long): Boolean =
    ts < Instant.now().truncatedTo(ChronoUnit.DAYS).toEpochMilli

  def nowAtNyse(): LocalDateTime =
    LocalDateTime.now(ZoneId.of("America/New_York"))
}
