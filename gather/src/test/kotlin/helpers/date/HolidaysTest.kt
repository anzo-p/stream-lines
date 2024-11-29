package helpers.date

import java.time.LocalDate
import net.anzop.gather.helpers.date.isHoliday
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HolidaysTest {

    @Test
    fun `LocalDate isHoliday() should return true for fixed holidays`() {
        val newYear = LocalDate.of(2026, 1, 1)
        val juneteenth = LocalDate.of(2026, 6, 19)
        val independenceDay = LocalDate.of(2026, 7, 4)
        val veteransDay = LocalDate.of(2026, 11, 11)
        val christmasEve = LocalDate.of(2026, 12, 24)
        val christmasDay = LocalDate.of(2026, 12, 25)

        listOf(newYear, juneteenth, independenceDay, veteransDay, christmasEve, christmasDay)
            .forEach { assertThat(it.isHoliday()).isTrue() }
    }

    @Test
    fun `LocalDate isHoliday() should return false for non-fixed-holidays`() {
        val newYearsEve = LocalDate.of(2026, 12, 31)
        val pastNewYear = LocalDate.of(2026, 1, 2)
        val juneteenthEve = LocalDate.of(2026, 6, 18)
        val pastJuneteenth = LocalDate.of(2026, 6, 20)
        val independenceDayEve = LocalDate.of(2026, 7, 3)
        val pastIndependenceDay = LocalDate.of(2026, 7, 5)
        val veteransDayEve = LocalDate.of(2026, 11, 10)
        val pastVeteransDay = LocalDate.of(2026, 11, 12)
        val dayBeforeChristmasEve = LocalDate.of(2026, 12, 23)
        val pastChristmas = LocalDate.of(2026, 12, 26)

        listOf(
            newYearsEve, pastNewYear,
            juneteenthEve, pastJuneteenth,
            independenceDayEve, pastIndependenceDay,
            veteransDayEve, pastVeteransDay,
            dayBeforeChristmasEve, pastChristmas
        )
            .forEach { assertThat(it.isHoliday()).isFalse() }
    }

    @Test
    fun `LocalDate isHoliday() should return true for variable holidays`() {
        val mlkDay = LocalDate.of(2026, 1, 19)
        val washingtonBirthday = LocalDate.of(2026, 2, 16)
        val memorialDay = LocalDate.of(2026, 5, 25)
        val laborDay = LocalDate.of(2026, 9, 7)
        val columbusDay = LocalDate.of(2026, 10, 12)
        val thanksgivingDay = LocalDate.of(2026, 11, 26)

        listOf(mlkDay, washingtonBirthday, memorialDay, laborDay, columbusDay, thanksgivingDay)
            .forEach { assertThat(it.isHoliday()).isTrue() }
    }

    @Test
    fun `LocalDate isHoliday() should return false for non-variable-holidays`() {
        val mlkDayEve = LocalDate.of(2026, 1, 18)
        val pastMlkDay = LocalDate.of(2026, 1, 20)
        val washingtonBirthdayEve = LocalDate.of(2026, 2, 15)
        val pastWashingtonBirthday = LocalDate.of(2026, 2, 17)
        val memorialDayEve = LocalDate.of(2026, 5, 24)
        val pastMemorialDay = LocalDate.of(2026, 5, 26)
        val laborDayEve = LocalDate.of(2026, 9, 6)
        val pastLaborDay = LocalDate.of(2026, 9, 8)
        val columbusDayEve = LocalDate.of(2026, 10, 11)
        val pastColumbusDay = LocalDate.of(2026, 10, 13)
        val thanksgivingDayEve = LocalDate.of(2026, 11, 25)
        val pastThanksgivingDay = LocalDate.of(2026, 11, 27)

        listOf(
            mlkDayEve, pastMlkDay,
            washingtonBirthdayEve, pastWashingtonBirthday,
            memorialDayEve, pastMemorialDay,
            laborDayEve, pastLaborDay,
            columbusDayEve, pastColumbusDay,
            thanksgivingDayEve, pastThanksgivingDay
        )
            .forEach { assertThat(it.isHoliday()).isFalse() }
    }
}
