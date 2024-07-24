package net.anzop.retro.helpers.date

import java.time.Instant
import java.time.LocalDate

fun nyseTradingHoursOr24h(date: LocalDate, onlyRegularTradingHours: Boolean): Pair<Instant, Instant>? =
    if (onlyRegularTradingHours) {
        nyseTradingHours
            .from(date.asAmericaNyToInstant())
            ?.let { (open, close) ->
                open to close.minusMillis(1)
        }
    } else {
        date.asAmericaNyToInstant().let { from ->
            from to from.plusOneDayAlmost()
        }
    }

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
        .filterNot { it.isWeekend() }
        .filterNot { it.isHoliday() }
}
