package net.anzop.gather.helpers.date

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime

data class TradingHours(
    val zoneId: ZoneId,
    val open: LocalTime,
    val close: LocalTime
) {
    fun isOpenAt(instant: Instant): Boolean {
        val localDate = instant.toLocalDate(zoneId)
        val localTime = ZonedDateTime.ofInstant(instant, zoneId).toLocalTime()

        val tradingHours = (localTime.equals(open) || localTime.isAfter(open)) &&
                localTime.isBefore(close)

        return !localDate.isWeekend() && !localDate.isHoliday() && tradingHours
    }

    fun from(date: LocalDate): Pair<Instant, Instant>? =
        if (!date.isWeekend() && !date.isHoliday()) {
            Pair(
                date.atTime(open).atZone(zoneId).toInstant(),
                date.atTime(close).atZone(zoneId).toInstant()
            )
        } else {
            null
        }

    fun from(instant: Instant): Pair<Instant, Instant>? =
        from(instant.toLocalDate(zoneId))
}

val nyseTradingHours = TradingHours(
    zoneId = ZoneId.of("America/New_York"),
    open = LocalTime.of(9, 30),
    close = LocalTime.of(16, 0)
)
