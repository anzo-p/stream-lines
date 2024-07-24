package helpers.date

import java.time.Instant
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import net.anzop.retro.helpers.date.asAmericaNyToInstant
import net.anzop.retro.helpers.date.isWeekend
import net.anzop.retro.helpers.date.toLocalDate
import net.anzop.retro.helpers.date.toOffsetDateTime
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class ExtensionsTest {

    @Test
    fun `LocalDate asAmericaNyToInstant() should return an instant in America New York timezone`() {
        val utcDate = LocalDate.of(2026, 1, 1)
        val expectedInstant = Instant.parse("2026-01-01T05:00:00Z")

        assertThat(utcDate.asAmericaNyToInstant()).isEqualTo(expectedInstant)
    }

    @Test
    fun `LocalDate asAmericaNyToInstant() should mind the daylight saving time`() {
        val utcDate = LocalDate.of(2026, 6, 1)
        val expectedInstant = Instant.parse("2026-06-01T04:00:00Z")

        assertThat(utcDate.asAmericaNyToInstant()).isEqualTo(expectedInstant)
    }

    @Test
    fun `LocalDate isWeekend() should return true only for saturday and sunday`() {
        val monday = LocalDate.of(2026, 1, 5)

        listOf(0, 1, 2, 3, 4).forEach { dayOfWeek ->
            assertThat(monday.plusDays(dayOfWeek.toLong()).isWeekend()).isFalse()
        }

        listOf(5, 6).forEach { dayOfWeek ->
            assertThat(monday.plusDays(dayOfWeek.toLong()).isWeekend()).isTrue()
        }
    }

    @Test
    fun `Instant toLocalDate() should return the date at the target time zone`() {
        val instant = Instant.parse("2026-01-01T00:00:00Z")
        val expectedUtcDate = LocalDate.of(2026, 1, 1)

        assertThat(instant.toLocalDate()).isEqualTo(expectedUtcDate)

        val nyInstant = Instant.parse("2026-01-01T05:00:00Z")
        val expectedNyDate = LocalDate.of(2026, 1, 1)

        assertThat(nyInstant.toLocalDate(ZoneId.of("America/New_York"))).isEqualTo(expectedNyDate)
    }

    @Test
    fun `Instant toLocalDate() should mind daylight saving time`() {
        val instant = Instant.parse("2026-06-01T00:00:00Z")
        val expectedUtcDateTime = LocalDate.of(2026, 6, 1)

        assertThat(instant.toLocalDate()).isEqualTo(expectedUtcDateTime)

        val nyInstant = Instant.parse("2026-06-01T04:00:00Z")
        val expectedNyDateTime = LocalDate.of(2026, 6, 1)

        assertThat(nyInstant.toLocalDate(ZoneId.of("America/New_York"))).isEqualTo(expectedNyDateTime)
    }

    @Test
    fun `Instant toOffsetDateTime() should return the date time at the target time zone`() {
        val instant = Instant.parse("2026-01-01T00:00:00Z")
        val expectedUtcDateTime = OffsetDateTime.parse("2026-01-01T00:00:00Z")

        assertThat(instant.toOffsetDateTime()).isEqualTo(expectedUtcDateTime)

        val nyInstant = Instant.parse("2026-01-01T00:00:00Z")
        val expectedNyDateTime = OffsetDateTime.parse("2025-12-31T19:00-05:00")

        assertThat(nyInstant.toOffsetDateTime(ZoneId.of("America/New_York"))).isEqualTo(expectedNyDateTime)
    }

    @Test
    fun `Instant toOffsetDateTime() should mind daylight saving time`() {
        val instant = Instant.parse("2026-06-01T00:00:00Z")
        val expectedUtcDateTime = OffsetDateTime.parse("2026-06-01T00:00:00Z")

        assertThat(instant.toOffsetDateTime()).isEqualTo(expectedUtcDateTime)

        val nyInstant = Instant.parse("2026-06-01T00:00:00Z")
        val expectedNyDateTime = OffsetDateTime.parse("2026-05-31T20:00-04:00")

        assertThat(nyInstant.toOffsetDateTime(ZoneId.of("America/New_York"))).isEqualTo(expectedNyDateTime)

    }
}