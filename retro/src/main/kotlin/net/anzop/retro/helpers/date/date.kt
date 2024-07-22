package net.anzop.retro.helpers.date

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit

fun LocalDate.asAmericaNyToInstant(): Instant =
    this.atStartOfDay(ZoneId.of("America/New_York")).toInstant()

fun LocalDate.toInstant(): Instant =
    this.atStartOfDay().toInstant(ZoneOffset.UTC)

fun Instant.plusOneDayAlmost(): Instant =
    this.plus(1, ChronoUnit.DAYS).minusMillis(1)

fun Instant.toLocalDate(): LocalDate =
    this.atZone(ZoneId.of("UTC")).toLocalDate()

fun Instant.toOffsetDateTime(): OffsetDateTime =
    OffsetDateTime.ofInstant(this, ZoneId.of("UTC"))

fun generateWeekdayRange(startDate: LocalDate, endDate: LocalDate): List<LocalDate> {
    val sequence = generateSequence(startDate) { date ->
        if (date.isBefore(endDate)) {
            date.plusDays(1)
        } else {
            null
        }
    }

    return sequence
        .toList()
        .filterNot { listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(it.dayOfWeek) }
        .filterNot { it.isHoliday() }
}
