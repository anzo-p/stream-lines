package dto.bars

import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset
import net.anzop.retro.dto.bars.BarDataDto
import net.anzop.retro.helpers.date.nyseTradingHours
import net.anzop.retro.model.Ticker
import net.anzop.retro.model.marketData.Measurement
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.math.absoluteValue
import kotlin.random.Random

internal class BarDataDtoTest {

    private val nyseHoursInstant = nyseTradingHours.from(LocalDate.of(2026, 1, 2))!!.first
    private val nyseHoursOffsetDateTime = OffsetDateTime.ofInstant(nyseHoursInstant, nyseTradingHours.zoneId)

    private val measurement = Measurement.SECURITY_REGULAR_PRICE_CHANGE_ARITHMETIC_DAILY

    private val ticker = Ticker(getRandomCaps(5), getRandomString())

    private val barDataInput = BarDataDto(
        closingPrice = Random.nextDouble(),
        highPrice = Random.nextDouble(),
        lowPrice = Random.nextDouble(),
        openingPrice = Random.nextDouble(),
        marketTimestamp = nyseHoursOffsetDateTime,
        volume = Random.nextLong().absoluteValue,
        volumeWeightedAvgPrice = Random.nextDouble(),
    )

    @Test
    fun `toModel should return BarData`() {
        val result = barDataInput.toModel(measurement, ticker)

        assertThat(result).isNotNull
        assertThat(result.measurement).isEqualTo(measurement)
        assertThat(result.ticker).isEqualTo(ticker.symbol)
        assertThat(result.company).isEqualTo(ticker.company)
        assertThat(result.marketTimestamp).isEqualTo(barDataInput.marketTimestamp.toInstant())
        assertThat(result.regularTradingHours).isTrue
        assertThat(result.closingPrice).isEqualTo(barDataInput.closingPrice)
        assertThat(result.highPrice).isEqualTo(barDataInput.highPrice)
        assertThat(result.lowPrice).isEqualTo(barDataInput.lowPrice)
        assertThat(result.openingPrice).isEqualTo(barDataInput.openingPrice)
        assertThat(result.volumeWeightedAvgPrice).isEqualTo(barDataInput.volumeWeightedAvgPrice)
        assertThat(result.totalTradingValue).isEqualTo(barDataInput.volumeWeightedAvgPrice * barDataInput.volume)
    }

    @Test
    fun `toModel should set nyse regular trading hours to false`() {
        val newYearsEve = OffsetDateTime.of(
            LocalDate.of(2026, 1, 1).atTime(0, 0),
            ZoneOffset.UTC
        )
        val result = barDataInput.copy(marketTimestamp = newYearsEve).toModel(measurement, ticker)

        assertThat(result.regularTradingHours).isFalse
    }

    @Test
    fun `toModel should throw IllegalArgumentException when validation fails on closingPrice`() {
        val exception = assertThrows<IllegalArgumentException> {
            barDataInput.copy(closingPrice = barDataInput.closingPrice * -1).toModel(measurement, ticker)
        }
        assertEquals("Validation failed for BarDataDto: 'closingPrice' must be positive", exception.message)
    }

    @Test
    fun `toModel should throw IllegalArgumentException when validation fails on highPrice`() {
        val exception = assertThrows<IllegalArgumentException> {
            barDataInput.copy(highPrice = barDataInput.highPrice * -1).toModel(measurement, ticker)
        }
        assertEquals("Validation failed for BarDataDto: 'highPrice' must be positive", exception.message)
    }

    @Test
    fun `toModel should throw IllegalArgumentException when validation fails on lowPrice`() {
        val exception = assertThrows<IllegalArgumentException> {
            barDataInput.copy(lowPrice = barDataInput.lowPrice * -1).toModel(measurement, ticker)
        }
        assertEquals("Validation failed for BarDataDto: 'lowPrice' must be positive", exception.message)
    }

    @Test
    fun `toModel should throw IllegalArgumentException when validation fails on openingPrice`() {
        val exception = assertThrows<IllegalArgumentException> {
            barDataInput.copy(openingPrice = barDataInput.openingPrice * -1).toModel(measurement, ticker)
        }
        assertEquals("Validation failed for BarDataDto: 'openingPrice' must be positive", exception.message)
    }

    @Test
    fun `toModel should throw IllegalArgumentException when validation fails on volume`() {
        val exception = assertThrows<IllegalArgumentException> {
            barDataInput.copy(volume = barDataInput.volume * -1).toModel(measurement, ticker)
        }
        assertEquals("Validation failed for BarDataDto: 'volume' must be positive", exception.message)
    }

    @Test
    fun `toModel should throw IllegalArgumentException when validation fails on volumeWeightedAvgPrice`() {
        val exception = assertThrows<IllegalArgumentException> {
            barDataInput.copy(volumeWeightedAvgPrice = barDataInput.volumeWeightedAvgPrice * -1).toModel(measurement, ticker)
        }
        assertEquals("Validation failed for BarDataDto: 'volumeWeightedAvgPrice' must be positive", exception.message)
    }

    private fun getRandomCaps(length: Int = 10): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }

    private fun getRandomString(length: Int = 10): String {
        val chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"
        return (1..length)
            .map { chars.random() }
            .joinToString("")
    }
}
