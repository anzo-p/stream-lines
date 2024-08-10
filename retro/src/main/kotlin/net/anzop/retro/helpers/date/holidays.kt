package net.anzop.retro.helpers.date

import java.time.DayOfWeek
import java.time.LocalDate

private data class VariableHoliday(val month: Int, val dayOfWeek: DayOfWeek, val n: Int)

private val fixedHolidays: Set<Pair<Int, Int>> = listOf(
    "1.1", //"New Year's Day
    "19.6", // Juneteenth National Independence Day
    "4.7", // Independence Day
    "11.11", // Veterans Day
    "24.12", // Christmas Eve
    "25.12", // Christmas Day
)
    .map {
        it.split(".")
            .let { (m, d) -> m.toInt() to d.toInt() }
    }
    .toSet()

private val variableHolidays: Set<VariableHoliday> = setOf(
    VariableHoliday(1, DayOfWeek.MONDAY, 3), // Martin Luther King Jr. Day
    VariableHoliday(2, DayOfWeek.MONDAY, 3), // Washington's Birthday
    VariableHoliday(5, DayOfWeek.MONDAY, -1), // Memorial Day
    VariableHoliday(9, DayOfWeek.MONDAY, 1), // Labor Day
    VariableHoliday(10, DayOfWeek.MONDAY, 2), // Columbus Day
    VariableHoliday(11, DayOfWeek.THURSDAY, 4), // Thanksgiving Day
)

fun LocalDate.isHoliday(): Boolean {
    val fixed = fixedHolidays.contains(this.dayOfMonth to this.monthValue)

    val variable = variableHolidays.any { (month, dayOfWeek, n) ->
        this.monthValue == month && this.dayOfWeek == dayOfWeek && run {
            if (n > 0) {
                (n - 1) * 7 < this.dayOfMonth && n * 7 >= this.dayOfMonth
            } else {
                this.dayOfMonth >= this.lengthOfMonth() - 7
            }
        }
    }

    return fixed || variable
}
