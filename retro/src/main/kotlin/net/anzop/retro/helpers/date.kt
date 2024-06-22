package net.anzop.retro.helpers

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

fun resolveStartDate(latestDateForTicker: Instant?, baseStartDate: LocalDate): OffsetDateTime? {
    val fromDate = latestDateForTicker?.atZone(ZoneOffset.UTC)?.toLocalDate() ?: baseStartDate

    val tilDate = LocalDate.now().run {
        if (dayOfWeek > DayOfWeek.FRIDAY) {
            minusDays((dayOfWeek.value - DayOfWeek.FRIDAY.value).toLong())
        } else {
            this
        }
    }

    return fromDate.takeIf { it.isBefore(tilDate) }?.atTime(LocalTime.MIDNIGHT)?.atOffset(ZoneOffset.UTC)
}
