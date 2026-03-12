package net.anzop.helpers

import java.time.{ZoneId, ZonedDateTime}

object DateHelpers {

  def nyseOpen(): Long = {
    val zoneNy = ZoneId.of("America/New_York")
    val openNy = ZonedDateTime.now(zoneNy).toLocalDate.atTime(9, 30).atZone(zoneNy)
    openNy.minusHours(1).toInstant.toEpochMilli
  }
}
