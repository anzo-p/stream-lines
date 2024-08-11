package services

import io.mockk.called
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import net.anzop.retro.config.AlpacaProps
import net.anzop.retro.helpers.date.getPreviousBankDay
import net.anzop.retro.helpers.date.toInstant
import net.anzop.retro.helpers.date.toLocalDate
import net.anzop.retro.model.marketData.Measurement
import net.anzop.retro.repository.dynamodb.CacheRepository
import net.anzop.retro.repository.influxdb.MarketDataFacade
import net.anzop.retro.service.IndexProcessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class IndexProcessorTest() {
    private val defaultMeasurement = Measurement.INDEX_EXTENDED_EQUAL_ARITHMETIC_DAILY
    private val defaultLocalDate = LocalDate.of(2026, 1, 1)
    private val mockAction = mockk<Runnable>{
        every { run() } returns Unit
    }

    private val testAlpacaProps = AlpacaProps().apply {
        earliestHistoricalDate = defaultLocalDate
    }
    private val mockCacheRepository = mockk<CacheRepository>{
        every { getIndexStaleFrom() } returns null
    }
    private val mockMarketDataFacade = mockk<MarketDataFacade>{
        every { getLatestIndexEntry(defaultMeasurement) } returns null
    }
    private val indexProcessor = IndexProcessor(
        alpacaProps = testAlpacaProps,
        cacheRepository = mockCacheRepository,
        marketDataFacade = mockMarketDataFacade
    )

    @Test
    fun `getStartDate should fallback to alpacaProps earliestHistoricalDate`() {
        val startDate = indexProcessor.resolveStartDate(
            measurement = defaultMeasurement,
            fallbackAction = { mockAction.run() }
        )

        assertThat(startDate).isEqualTo(testAlpacaProps.earliestHistoricalDate)
        verify(exactly = 1) { mockAction.run() }
    }

    @Test
    fun `getStartDate should use index stale minus one day if latest index entry is null`() {
        val indexStaleFrom = defaultLocalDate

        every { mockCacheRepository.getIndexStaleFrom() } returns indexStaleFrom

        val startDate = indexProcessor.resolveStartDate(
            measurement = defaultMeasurement,
            fallbackAction = { mockAction.run() }
        )

        assertThat(startDate).isEqualTo(indexStaleFrom.getPreviousBankDay())
        verify { mockAction wasNot called }
    }

    @Test
    fun `getStartDate should use latest index entry minus one day if index stale date is null`() {
        val latestIndexEntry = defaultLocalDate.toInstant()

        every { mockMarketDataFacade.getLatestIndexEntry(defaultMeasurement) } returns latestIndexEntry

        val startDate = indexProcessor.resolveStartDate(
            measurement = defaultMeasurement,
            fallbackAction = { mockAction.run() }
        )

        assertThat(startDate).isEqualTo(latestIndexEntry.toLocalDate().getPreviousBankDay())
        verify { mockAction wasNot called }
    }

    @Test
    fun `getStartDate should use index stale minus one day if earlier than latest index entry`() {
        val indexStaleFrom = defaultLocalDate
        val latestIndexEntry = indexStaleFrom.plusDays(1).toInstant()

        every { mockCacheRepository.getIndexStaleFrom() } returns indexStaleFrom
        every { mockMarketDataFacade.getLatestIndexEntry(defaultMeasurement) } returns latestIndexEntry

        val startDate = indexProcessor.resolveStartDate(
            measurement = defaultMeasurement,
            fallbackAction = { mockAction.run() }
        )

        assertThat(startDate).isEqualTo(indexStaleFrom.getPreviousBankDay())
        verify { mockAction wasNot called }
    }

    @Test
    fun `getStartDate should use latest index entry minus one day if earlier than index stale date`() {
        val indexStaleFrom = defaultLocalDate
        val latestIndexEntry = indexStaleFrom.minusDays(1).toInstant()

        every { mockCacheRepository.getIndexStaleFrom() } returns indexStaleFrom
        every { mockMarketDataFacade.getLatestIndexEntry(defaultMeasurement) } returns latestIndexEntry

        val startDate = indexProcessor.resolveStartDate(
            measurement = defaultMeasurement,
            fallbackAction = { mockAction.run() }
        )

        assertThat(startDate).isEqualTo(latestIndexEntry.toLocalDate().getPreviousBankDay())
        verify { mockAction wasNot called }
    }
}
