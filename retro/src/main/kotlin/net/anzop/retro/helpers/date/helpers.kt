package net.anzop.retro.helpers.date

import java.time.Instant
import java.time.LocalDate

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

fun minOfOpt(instant1: Instant?, instant2: Instant?): Instant? =
    when {
        instant1 == null -> instant2
        instant2 == null -> instant1
        else -> minOf(instant1, instant2)
    }

fun minOfOptWithFallback(
    instant1: Instant?,
    instant2: Instant?,
    fallbackAction: () -> Unit
): Instant? =
    minOfOpt(instant1, instant2) ?: run {
        fallbackAction()
        null
    }

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
