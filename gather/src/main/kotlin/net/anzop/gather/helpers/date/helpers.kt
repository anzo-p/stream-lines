package net.anzop.gather.helpers.date

import java.time.Instant
import java.time.LocalDate
import net.anzop.gather.model.financials.FinancialPeriod
import net.anzop.gather.model.financials.ReportPeriodType

fun generateFinancialPeriodRange(periodType: ReportPeriodType): List<Pair<Int, FinancialPeriod>> =
    when (periodType) {
        ReportPeriodType.ANNUAL -> generateFiscalYearsRange()
        ReportPeriodType.QUARTER -> generateQuartersRange()
        else ->emptyList()
    }

fun generateFiscalYearsRange(
    startYear: Int = 2016,
    stopYear: Int = LocalDate.now().minusYears(1).year
): List<Pair<Int, FinancialPeriod>> =
    generateSequence(startYear) { it + 1 }
        .takeWhile { it <= stopYear }
        .map { it to FinancialPeriod.ANNUAL }
        .toList()

fun generateQuartersRange(
    startYear: Int = 2016,
): List<Pair<Int, FinancialPeriod>> {
    val now = LocalDate.now()
    val lastQuarter = (now.monthValue - 1) / 3
    val stopYear = if (lastQuarter == 0) now.year - 1 else now.year
    val stopQuarter = if (lastQuarter == 0) 4 else lastQuarter

    return generateSequence(startYear) { it + 1 }
        .takeWhile { it < stopYear || (it == stopYear && lastQuarter <= stopQuarter - 1) }
        .flatMap {
            FinancialPeriod.entries
                .filterNot { it == FinancialPeriod.ANNUAL }
                .map { period -> it to period }
        }
        .toList()
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
