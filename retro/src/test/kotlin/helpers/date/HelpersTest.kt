package helpers.date

import java.time.Instant
import java.time.LocalDate
import net.anzop.retro.helpers.date.generateWeekdayRange
import net.anzop.retro.helpers.date.nyseTradingHoursOr24h
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class HelpersTest {

    @Test
    fun `nyseTradingHoursOr24h should return full America New York day when not regular trading hours`() {
        val date = LocalDate.of(2026, 1, 1)
        val expectedOpen = Instant.parse("2026-01-01T05:00:00Z")
        val expectedClose = Instant.parse("2026-01-02T04:59:59.999Z")

        val result = nyseTradingHoursOr24h(date, onlyRegularTradingHours = false)

        assertThat(result).isEqualTo(expectedOpen to expectedClose)
    }

    @Test
    fun `nyseTradingHoursOr24h should return regular trading hours for a given date`() {
        val date = LocalDate.of(2026, 1, 2)
        val expectedOpen = Instant.parse("2026-01-02T14:30:00Z")
        val expectedClose = Instant.parse("2026-01-02T20:59:59.999Z")

        val result = nyseTradingHoursOr24h(date, onlyRegularTradingHours = true)

        assertThat(result).isEqualTo(expectedOpen to expectedClose)
    }

    @Test
    fun `nyseTradingHoursOr24h should return null for trading hours when weekend`() {
        val date = LocalDate.of(2026, 1, 1)
        assertThat(nyseTradingHoursOr24h(date, onlyRegularTradingHours = true)).isNull()
    }

    @Test
    fun `nyseTradingHoursOr24h should return null for trading hours when holiday`() {
        val date = LocalDate.of(2026, 1, 1)
        assertThat(nyseTradingHoursOr24h(date, onlyRegularTradingHours = true)).isNull()
    }

    @Test
    fun `generateWeekdayRange should return weekdays between two dates`() {
        val startDate = LocalDate.of(2026, 11, 9)
        val endDate = LocalDate.of(2026, 11, 30)

        val range = generateWeekdayRange(startDate, endDate)

        assertThat(range.size).isEqualTo(14)
        assertThat(LocalDate.of(2026, 11, 11) in range).isFalse() // Veterans Day
        assertThat(LocalDate.of(2026, 11, 14) in range).isFalse() // Saturday
        assertThat(LocalDate.of(2026, 11, 15) in range).isFalse() // Sunday
        assertThat(LocalDate.of(2026, 11, 21) in range).isFalse() // Saturday
        assertThat(LocalDate.of(2026, 11, 22) in range).isFalse() // Sunday
        assertThat(LocalDate.of(2026, 11, 26) in range).isFalse() // Thanksgiving Day
    }

    @Test
    fun `generateWeekdayRange should exclude last weekday type of holiday rules`() {
        val startDate = LocalDate.of(2026, 5, 25)
        val endDate = LocalDate.of(2026, 5, 26)

        val range = generateWeekdayRange(startDate, endDate)

        assertThat(range.size).isEqualTo(1)
        assertThat(LocalDate.of(2026, 5, 25) in range).isFalse() // Memorial Day
    }
}