package net.anzop.retro.repository.influxdb

import java.time.Instant
import java.time.LocalDate
import net.anzop.retro.helpers.date.nyseTradingHoursOr24h
import net.anzop.retro.helpers.date.toInstant
import net.anzop.retro.model.marketData.BarData
import net.anzop.retro.model.marketData.MarketData
import net.anzop.retro.model.marketData.Measurement
import net.anzop.retro.model.marketData.PriceChange
import net.anzop.retro.repository.influxdb.repository.MarketDataRepository
import org.springframework.stereotype.Component

@Component
class MarketDataFacade (
    private val marketDataRepository: MarketDataRepository
) {
    fun <T> save(entity: T) =
        marketDataRepository.save(listOf(entity))

    fun <T> save(entities: List<T>) =
        marketDataRepository.save(entities)

    fun <T> saveAsync(entities: List<T>) =
        marketDataRepository.saveAsync(entities)

    fun getEarliestSourceBarDataEntry(ticker: String): Instant? =
        marketDataRepository.getFirstMeasurementTime(
            measurement = Measurement.SECURITY_RAW_SEMI_HOURLY,
            ticker = ticker
        )

    fun getLatestSourceBarDataEntry(ticker: String): Instant? =
        listOf(false, true).mapNotNull {
            marketDataRepository.getLatestMeasurementTime(
                measurement = Measurement.SECURITY_RAW_SEMI_HOURLY,
                ticker = ticker,
                regularTradingHours = it
            )
        }.maxOrNull()

    fun getLatestIndexEntry(measurement: Measurement): Instant? =
        marketDataRepository.getLatestMeasurementTime(
            measurement = measurement,
            ticker = "INDEX"
        )

    fun getSourceBarData(
        date: LocalDate,
        onlyRegularTradingHours: Boolean,
        ticker: String? = null
    ): List<BarData> {
        val (from, til) = nyseTradingHoursOr24h(date, onlyRegularTradingHours) ?: return emptyList()

        return getMeasurements(
            measurement = Measurement.SECURITY_RAW_SEMI_HOURLY,
            from = from,
            til = til,
            ticker = ticker,
            clazz = BarData::class.java
        )
    }

    fun getIndexValueAt(measurement: Measurement, date: LocalDate): Double? =
        marketDataRepository.getFirstMeasurement(
            measurement = measurement,
            ticker = "INDEX",
            since = date.toInstant(),
            clazz = PriceChange::class.java
        )?.priceChangeAvg

    fun <T : MarketData> getMeasurements(
        measurement: Measurement,
        from: Instant,
        til: Instant? = null,
        ticker: String? = null,
        clazz: Class<T>
    ): List<T> =
        marketDataRepository.getMeasurements(
            measurement = measurement,
            from = from,
            til = til,
            ticker = ticker,
            clazz = clazz
        )
}
