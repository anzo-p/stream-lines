package net.anzop.gather.helpers.date

import java.time.DayOfWeek
import java.time.LocalDate

private data class IrregularHoliday(val date: LocalDate)

private data class VariableRegularHoliday(val month: Int, val dayOfWeek: DayOfWeek, val n: Int)

private val irregularHolidays: Set<IrregularHoliday> = setOf(
    IrregularHoliday(LocalDate.of(2016, 3, 25)), // Good Friday
    IrregularHoliday(LocalDate.of(2017, 4, 14)), // Good Friday
    IrregularHoliday(LocalDate.of(2018, 3, 30)), // Good Friday
    IrregularHoliday(LocalDate.of(2019, 4, 19)), // Good Friday
    IrregularHoliday(LocalDate.of(2020, 4, 10)), // Good Friday
    IrregularHoliday(LocalDate.of(2021, 4, 2)),  // Good Friday
    IrregularHoliday(LocalDate.of(2022, 4, 15)), // Good Friday
    IrregularHoliday(LocalDate.of(2023, 4, 7)),  // Good Friday
    IrregularHoliday(LocalDate.of(2024, 3, 29)), // Good Friday
    IrregularHoliday(LocalDate.of(2025, 4, 18)), // Good Friday
)

private val fixedRegularHolidays: Set<Pair<Int, Int>> = setOf(
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

private val variableHolidays: Set<VariableRegularHoliday> = setOf(
    VariableRegularHoliday(1, DayOfWeek.MONDAY, 3),    // Martin Luther King Jr. Day
    VariableRegularHoliday(2, DayOfWeek.MONDAY, 3),    // Washington's Birthday
    VariableRegularHoliday(5, DayOfWeek.MONDAY, -1),   // Memorial Day
    VariableRegularHoliday(9, DayOfWeek.MONDAY, 1),    // Labor Day
    VariableRegularHoliday(10, DayOfWeek.MONDAY, 2),   // Columbus Day
    VariableRegularHoliday(11, DayOfWeek.THURSDAY, 4), // Thanksgiving Day
)

fun LocalDate.isHoliday(): Boolean {
    val fixed = fixedRegularHolidays.contains(this.dayOfMonth to this.monthValue)

    val variable = variableHolidays.any { (month, dayOfWeek, n) ->
        this.monthValue == month && this.dayOfWeek == dayOfWeek && run {
            if (n > 0) {
                (n - 1) * 7 < this.dayOfMonth && n * 7 >= this.dayOfMonth
            } else {
                this.dayOfMonth >= this.lengthOfMonth() - 7
            }
        }
    }

    val irregular = irregularHolidays.any { it.date.equals(this) }

    return fixed || variable || irregular
}
