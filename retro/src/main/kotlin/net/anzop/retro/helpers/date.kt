package net.anzop.retro.helpers

import java.time.DayOfWeek
import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId

fun LocalDate.toInstant(): Instant =
    this.atStartOfDay().atZone(ZoneId.of("UTC")).toInstant()

fun Instant.toOffsetDateTime(): OffsetDateTime =
    OffsetDateTime.ofInstant(this, ZoneId.of("UTC"))

fun genWeekdayRange(startDate: LocalDate, endDate: LocalDate): List<LocalDate> =
    generateSequence(startDate) { date -> if (date.isBefore(endDate)) date.plusDays(1) else null }
        .toList()
        .filterNot { listOf(DayOfWeek.SATURDAY, DayOfWeek.SUNDAY).contains(it.dayOfWeek) }
