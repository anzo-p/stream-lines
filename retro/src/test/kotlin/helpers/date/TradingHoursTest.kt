package helpers.date

import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import net.anzop.retro.helpers.date.TradingHours
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class TradingHoursTest {

    @Test
    fun `isOpenAt() should return false on weekend`() {
        val tradingHours = TradingHours(
            zoneId = ZoneId.of("UTC"),
            open = LocalTime.of(0, 0),
            close = LocalTime.of(12, 0)
        )
        val weekendBegins = Instant.parse("2026-01-03T00:00:00.000Z")
        val weekendEnding = Instant.parse("2026-01-05T23:59:59.999999Z")
        val afterWeekend = Instant.parse("2026-01-05T00:00:00.000Z")

        assertThat(tradingHours.isOpenAt(weekendBegins)).isFalse()
        assertThat(tradingHours.isOpenAt(weekendEnding)).isFalse()
        assertThat(tradingHours.isOpenAt(afterWeekend)).isTrue()
    }

    @Test
    fun `isOpenAt() should return false on holiday`() {
        val tradingHours = TradingHours(
            zoneId = ZoneId.of("UTC"),
            open = LocalTime.of(0, 0),
            close = LocalTime.of(12, 0)
        )
        val holidayBegins = Instant.parse("2026-01-01T00:00:00.000Z")
        val holidayEnding = Instant.parse("2025-12-31T23:59:59.999999Z")
        val afterHoliday = Instant.parse("2026-01-02T00:00:00.000Z")

        assertThat(tradingHours.isOpenAt(holidayBegins)).isFalse()
        assertThat(tradingHours.isOpenAt(holidayEnding)).isFalse()
        assertThat(tradingHours.isOpenAt(afterHoliday)).isTrue()
    }

    @Test
    fun `isOpenAt() should follow time zone`() {
        val tradingHours = TradingHours(
            zoneId = ZoneId.of("America/New_York"),
            open = LocalTime.of(12, 30),
            close = LocalTime.of(12, 31)
        )
        val beforeOpen = Instant.parse("2026-01-05T17:29:59.999999Z")
        val opening = Instant.parse("2026-01-05T17:30:00.000Z")
        val closing = Instant.parse("2026-01-05T17:30:59.999999Z")
        val afterClose = Instant.parse("2026-01-05T17:31:00.000Z")

        assertThat(tradingHours.isOpenAt(beforeOpen)).isFalse()
        assertThat(tradingHours.isOpenAt(opening)).isTrue()
        assertThat(tradingHours.isOpenAt(closing)).isTrue()
        assertThat(tradingHours.isOpenAt(afterClose)).isFalse()
    }

    @Test
    fun `isOpenAt() should follow daylight saving time`() {
        val tradingHours = TradingHours(
            zoneId = ZoneId.of("America/New_York"),
            open = LocalTime.of(12, 30),
            close = LocalTime.of(12, 31)
        )
        val beforeOpen = Instant.parse("2026-06-01T17:29:59.999999Z")
        val opening = Instant.parse("2026-06-01T16:30:00.000Z")
        val closing = Instant.parse("2026-06-01T16:30:59.999999Z")
        val afterClose = Instant.parse("2026-06-01T16:31:00.000Z")

        assertThat(tradingHours.isOpenAt(beforeOpen)).isFalse()
        assertThat(tradingHours.isOpenAt(opening)).isTrue()
        assertThat(tradingHours.isOpenAt(closing)).isTrue()
        assertThat(tradingHours.isOpenAt(afterClose)).isFalse()
    }

    @Test
    fun `from LocalDate should return a pair of open close times at target timezone for given LocalDate`() {
        val tradingHours = TradingHours(
            zoneId = ZoneId.of("America/New_York"),
            open = LocalTime.of(12, 30),
            close = LocalTime.of(12, 31)
        )
        val date = LocalDate.of(2026, 1, 2)

        val (open, close) = tradingHours.from(date)!!

        assertThat(open).isEqualTo(Instant.parse("2026-01-02T17:30:00.000Z"))
        assertThat(close).isEqualTo(Instant.parse("2026-01-02T17:31:00.000Z"))
    }

    @Test
    fun `from() LocalDate should return null when weekend`() {
        val tradingHours = TradingHours(
            zoneId = ZoneId.of("America/New_York"),
            open = LocalTime.of(12, 30),
            close = LocalTime.of(12, 31)
        )
        val date = LocalDate.of(2026, 1, 4)

        assertThat(tradingHours.from(date)).isNull()
    }

    @Test
    fun `from() LocalDate should return null when holiday`() {
        val tradingHours = TradingHours(
            zoneId = ZoneId.of("America/New_York"),
            open = LocalTime.of(12, 30),
            close = LocalTime.of(12, 31)
        )
        val date = LocalDate.of(2026, 1, 1)

        assertThat(tradingHours.from(date)).isNull()
    }

    @Test
    fun `from() Instant should return a pair of open close times at target timezone`() {
        val tradingHours = TradingHours(
            zoneId = ZoneId.of("America/New_York"),
            open = LocalTime.of(12, 30),
            close = LocalTime.of(12, 31)
        )
        val midnightAtTargetZone = Instant.parse("2026-01-06T05:00:00.000Z")
        val expectedOpen = Instant.parse("2026-01-06T17:30:00.000Z")
        val expectedClose = Instant.parse("2026-01-06T17:31:00.000Z")

        val (open, close) = tradingHours.from(midnightAtTargetZone)!!

        assertThat(open).isEqualTo(expectedOpen)
        assertThat(close).isEqualTo(expectedClose)

        val (open2, close2) = tradingHours.from(midnightAtTargetZone.minusNanos(1))!!

        assertThat(open2).isEqualTo(expectedOpen.minus(1, ChronoUnit.DAYS))
        assertThat(close2).isEqualTo(expectedClose.minus(1, ChronoUnit.DAYS))
    }

    @Test
    fun `from() should mind daylight saving time`() {
        val tradingHours = TradingHours(
            zoneId = ZoneId.of("America/New_York"),
            open = LocalTime.of(12, 30),
            close = LocalTime.of(12, 31)
        )
        val midnightAtTargetZone = Instant.parse("2026-06-02T04:00:00.000Z")
        val expectedOpen = Instant.parse("2026-06-02T16:30:00.000Z")
        val expectedClose = Instant.parse("2026-06-02T16:31:00.000Z")

        val (open, close) = tradingHours.from(midnightAtTargetZone)!!

        assertThat(open).isEqualTo(expectedOpen)
        assertThat(close).isEqualTo(expectedClose)

        val (open2, close2) = tradingHours.from(midnightAtTargetZone.minusNanos(1))!!

        assertThat(open2).isEqualTo(expectedOpen.minus(1, ChronoUnit.DAYS))
        assertThat(close2).isEqualTo(expectedClose.minus(1, ChronoUnit.DAYS))
    }

    @Test
    fun `from() should return null when weekend or holiday`() {
        val tradingHours = TradingHours(
            zoneId = ZoneId.of("America/New_York"),
            open = LocalTime.of(12, 30),
            close = LocalTime.of(12, 31)
        )
        val weekend = Instant.parse("2026-01-03T05:00:00.000Z")
        val holiday = Instant.parse("2026-01-01T05:00:00.000Z")

        assertThat(tradingHours.from(weekend)).isNull()
        assertThat(tradingHours.from(holiday)).isNull()
    }
}
