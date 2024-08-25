package net.anzop.retro.helpers.date

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

private val defaultZoneId = ZoneId.of("UTC")

fun LocalDate.asAmericaNyToInstant(): Instant =
    this.atStartOfDay(ZoneId.of("America/New_York")).toInstant()

fun LocalDate.isWeekend() =
    this.dayOfWeek in listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY)

fun LocalDate.getPreviousBankDay(limit: Int = 7): LocalDate =
    this.minusDays(1).let {
        return if (limit == 0 || (!it.isWeekend() && !it.isHoliday())) {
            it
        } else {
            it.getPreviousBankDay(limit - 1)
        }

    }

fun LocalDate.toInstant(zoneOffset: ZoneOffset? = ZoneOffset.UTC): Instant =
    this.atStartOfDay().toInstant(zoneOffset)

fun Instant.plusOneDayAlmost(): Instant =
    this.plus(1, ChronoUnit.DAYS).minusMillis(1)

fun Instant.toLocalDate(zoneId: ZoneId? = defaultZoneId): LocalDate =
    this.atZone(zoneId).toLocalDate()

fun Instant.toOffsetDateTime(zoneId: ZoneId? = defaultZoneId): OffsetDateTime =
    OffsetDateTime.ofInstant(this, zoneId)
