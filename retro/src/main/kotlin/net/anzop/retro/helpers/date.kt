package net.anzop.retro.helpers

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.ZoneOffset

fun LocalDate.toInstantUtc(): Instant =
    this.atStartOfDay().toInstant(ZoneOffset.UTC)

fun Instant.toLocalDate(): LocalDate =
    this.atZone(ZoneId.of("UTC")).toLocalDate()

fun Instant.toOffsetDateTimeUtc(): OffsetDateTime =
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
}
