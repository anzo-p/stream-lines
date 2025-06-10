package services

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import net.anzop.gather.config.AlpacaProps
import net.anzop.gather.helpers.date.getPreviousBankDay
import net.anzop.gather.helpers.date.toInstant
import net.anzop.gather.helpers.date.toLocalDate
import net.anzop.gather.model.marketData.Measurement
import net.anzop.gather.repository.dynamodb.IndexMemberRepository
import net.anzop.gather.repository.dynamodb.IndexStaleRepository
import net.anzop.gather.repository.influxdb.MarketDataFacade
import net.anzop.gather.service.IndexProcessor
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

internal class IndexProcessorTest() {
    private val defaultMeasurement = Measurement.INDEX_EXTENDED_EQUAL_ARITHMETIC_DAILY
    private val defaultLocalDate = LocalDate.of(2026, 1, 1)
    private val testAlpacaProps = AlpacaProps().apply {
        earliestHistoricalDate = defaultLocalDate
    }
    private val mockIndexStaleRepository = mockk<IndexStaleRepository>{
        every { getIndexStaleFrom() } returns null
        every { deleteIndexStaleFrom() } just Runs
    }
    private val mockIndexMemberRepository = mockk<IndexMemberRepository>()
    private val mockMarketDataFacade = mockk<MarketDataFacade>{
        every { getLatestIndexEntry(defaultMeasurement) } returns null
    }
    private val indexProcessor = IndexProcessor(
        alpacaProps = testAlpacaProps,
        indexStaleRepository = mockIndexStaleRepository,
        indexMemberRepository = mockIndexMemberRepository,
        indexMemberCreator = mockk(),
        marketDataFacade = mockMarketDataFacade
    )

    @Test
    fun `getStartDate should fallback to alpacaProps earliestHistoricalDate`() {
        val startDate = indexProcessor.resolveStartDate(defaultMeasurement)

        assertThat(startDate).isEqualTo(testAlpacaProps.earliestHistoricalDate)
        verify(exactly = 1) { mockIndexStaleRepository.deleteIndexStaleFrom() }
    }

    @Test
    fun `getStartDate should use index stale minus one day if latest index entry is null`() {
        val indexStaleFrom = defaultLocalDate

        every { mockIndexStaleRepository.getIndexStaleFrom() } returns indexStaleFrom

        val startDate = indexProcessor.resolveStartDate(defaultMeasurement)

        assertThat(startDate).isEqualTo(indexStaleFrom.getPreviousBankDay())
        verify(exactly = 1) { mockIndexStaleRepository.getIndexStaleFrom() }
        verify(exactly = 0) { mockIndexStaleRepository.deleteIndexStaleFrom() }
    }

    @Test
    fun `getStartDate should use latest index entry minus one day if index stale date is null`() {
        val latestIndexEntry = defaultLocalDate.toInstant()

        every { mockMarketDataFacade.getLatestIndexEntry(defaultMeasurement) } returns latestIndexEntry

        val startDate = indexProcessor.resolveStartDate(defaultMeasurement)

        assertThat(startDate).isEqualTo(latestIndexEntry.toLocalDate().getPreviousBankDay())
        verify(exactly = 1) { mockIndexStaleRepository.getIndexStaleFrom() }
        verify(exactly = 0) { mockIndexStaleRepository.deleteIndexStaleFrom() }
    }

    @Test
    fun `getStartDate should use index stale minus one day if earlier than latest index entry`() {
        val indexStaleFrom = defaultLocalDate
        val latestIndexEntry = indexStaleFrom.plusDays(1).toInstant()

        every { mockIndexStaleRepository.getIndexStaleFrom() } returns indexStaleFrom
        every { mockMarketDataFacade.getLatestIndexEntry(defaultMeasurement) } returns latestIndexEntry

        val startDate = indexProcessor.resolveStartDate(defaultMeasurement)

        assertThat(startDate).isEqualTo(indexStaleFrom.getPreviousBankDay())
        verify(exactly = 1) { mockIndexStaleRepository.getIndexStaleFrom() }
        verify(exactly = 0) { mockIndexStaleRepository.deleteIndexStaleFrom() }
    }

    @Test
    fun `getStartDate should use latest index entry minus one day if earlier than index stale date`() {
        val indexStaleFrom = defaultLocalDate
        val latestIndexEntry = indexStaleFrom.minusDays(1).toInstant()

        every { mockIndexStaleRepository.getIndexStaleFrom() } returns indexStaleFrom
        every { mockMarketDataFacade.getLatestIndexEntry(defaultMeasurement) } returns latestIndexEntry

        val startDate = indexProcessor.resolveStartDate(defaultMeasurement)

        assertThat(startDate).isEqualTo(latestIndexEntry.toLocalDate().getPreviousBankDay())
        verify(exactly = 1) { mockIndexStaleRepository.getIndexStaleFrom() }
        verify(exactly = 0) { mockIndexStaleRepository.deleteIndexStaleFrom() }
    }
}
