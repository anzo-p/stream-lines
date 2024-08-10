package net.anzop.retro.service

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import net.anzop.retro.config.AlpacaProps
import net.anzop.retro.helpers.date.generateWeekdayRange
import net.anzop.retro.helpers.date.getPreviousBankDay
import net.anzop.retro.helpers.date.toInstant
import net.anzop.retro.helpers.date.toLocalDate
import net.anzop.retro.model.IndexMember
import net.anzop.retro.model.PrevDayData
import net.anzop.retro.model.marketData.BarData
import net.anzop.retro.model.marketData.Measurement
import net.anzop.retro.model.marketData.PriceChange
import net.anzop.retro.model.marketData.geometricMean
import net.anzop.retro.model.marketData.mean
import net.anzop.retro.repository.dynamodb.CacheRepository
import net.anzop.retro.repository.influxdb.MarketDataFacade
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private typealias IndexMembers = MutableMap<String, IndexMember>

@Service
class IndexProcessor(
    private val alpacaProps: AlpacaProps,
    private val cacheRepository: CacheRepository,
    private val marketDataFacade: MarketDataFacade,
) {
    private val logger = LoggerFactory.getLogger(IndexProcessor::class.java)

    private var asyncRecordsToInsert = mutableListOf<Any>()

    fun run() {
        Measurement.indexMeasurements.forEach(this::process)

        cacheRepository.deleteIndexStaleFrom()
        marketDataFacade.saveAsync(asyncRecordsToInsert)
        asyncRecordsToInsert.clear()
    }

    private fun process(measurement: Measurement) {
        val startDate = cacheRepository
            .getIndexStaleFrom()
            ?.getPreviousBankDay()
            ?: run {
                cacheRepository.deleteMemberSecurities(measurement)
                alpacaProps.earliestHistoricalDate
            }
        logger.info("Processing index for ${measurement.code} from $startDate")

        val initialIndexValue = marketDataFacade.getIndexValueAt(measurement, startDate) ?: 1.0
        logger.info("Starting Index Value is: $initialIndexValue")

        val processingPeriod = generateWeekdayRange(
            startDate = startDate,
            endDate = Instant.now().toLocalDate()
        )
        logger.info("${processingPeriod.size} days to process the index for")

        val latestIndexValue = loop(measurement, processingPeriod, initialIndexValue)
        logger.info("Final Index Value is: $latestIndexValue")
    }

    private fun loop(
        measurement: Measurement,
        processingPeriod: List<LocalDate>,
        initialIndexValue: Double,
    ): Double {
        val securities = cacheRepository.getMemberSecurities(measurement).toMutableMap()

        val initialDay = processingPeriod.first().getPreviousBankDay()

        val initialPrices = marketDataFacade
            .getSourceBarData(
                date = initialDay,
                onlyRegularTradingHours = Measurement.regularHours(measurement)
            )
            .associate { it.ticker to it.volumeWeightedAvgPrice }

        val latestIndexValue = processingPeriod.fold(initialIndexValue) { currIndexValue, date ->
            if (ChronoUnit.DAYS.between(alpacaProps.earliestHistoricalDate, date) % 61 == 0L) {
                logger.info("Processing ${measurement.code}. Current date: $date, current index value: $currIndexValue")
            }

            val bars = marketDataFacade.getSourceBarData(
                date = date,
                onlyRegularTradingHours = Measurement.regularHours(measurement)
            )

            bars.forEach { bar ->
                securities.computeIfAbsent(bar.ticker) {

                    validateMember(bar.ticker, date)
                    IndexMember(
                        ticker = bar.ticker,
                        measurement = measurement,
                        indexValueWhenIntroduced = currIndexValue,
                        introductionPrice = bar.volumeWeightedAvgPrice,
                        prevDayData = PrevDayData(
                            date = date,
                            avgPrice = bar.volumeWeightedAvgPrice
                        )
                    )
                }

                if (date == processingPeriod.first()) {
                    securities.keys.forEach { ticker ->
                        securities[ticker] = securities[ticker]!!.copy(
                            prevDayData = PrevDayData(
                                date = initialDay,
                                avgPrice = initialPrices[ticker] ?: bar.volumeWeightedAvgPrice
                            )
                        )
                    }
                }
            }

            val priceChanges = processBars(measurement, securities, bars, date)
            asyncRecordsToInsert.addAll(priceChanges)

            resolveIndexValue(measurement, priceChanges, currIndexValue)
        }

        cacheRepository.storeMemberSecurities(measurement, securities)

        return latestIndexValue
    }

    private fun processBars(
        measurement: Measurement,
        securities: IndexMembers,
        bars: List<BarData>,
        indexDate: LocalDate
    ): List<PriceChange> =
        bars.mapNotNull { bar ->
            check(bar.regularTradingHours || !Measurement.regularHours(measurement)) {
                "bar data for ticker: ${bar.ticker} on day: $indexDate contains extended hours trades\n" +
                    bar.toString()
            }

            securities[bar.ticker]?.let { entry ->
                fun normalize(price: Double): Double =
                    (price / entry.introductionPrice) * entry.indexValueWhenIntroduced

                securities[bar.ticker] = entry.copy(
                    prevDayData = PrevDayData(
                        date = indexDate,
                        avgPrice = bar.volumeWeightedAvgPrice
                    )
                )

                PriceChange(
                    measurement = Measurement.securitiesForIndex(measurement),
                    company = bar.company,
                    ticker = bar.ticker,
                    regularTradingHours = Measurement.regularHours(measurement),
                    marketTimestamp = indexDate.toInstant(),
                    priceChangeOpen = normalize(bar.openingPrice),
                    priceChangeClose = normalize(bar.closingPrice),
                    priceChangeHigh = normalize(bar.highPrice),
                    priceChangeLow = normalize(bar.lowPrice),
                    priceChangeAvg = normalize(bar.volumeWeightedAvgPrice),
                    prevPriceChangeAvg = normalize(entry.prevDayData.avgPrice),
                    totalTradingValue = bar.totalTradingValue
                )
            }
        }

    private fun resolveIndexValue(
        measurement: Measurement,
        priceChanges: List<PriceChange>,
        indexValue: Double
    ): Double =
        priceChanges.takeIf { it.isNotEmpty() }
            ?.let { createIndex(measurement, it) }
            ?.priceChangeAvg
            ?: indexValue

    private fun createIndex(measurement: Measurement, priceChanges: List<PriceChange>): PriceChange {
        val indexBar = calculateIndex(measurement, priceChanges)
            .copy(
                regularTradingHours = Measurement.regularHours(measurement),
                measurement = measurement,
                company = "INDEX",
                ticker = "INDEX"
            )

        marketDataFacade.save(indexBar)

        return indexBar
    }

    private fun calculateIndex(measurement: Measurement, priceChanges: List<PriceChange>): PriceChange =
        when (measurement) {
            Measurement.INDEX_REGULAR_EQUAL_ARITHMETIC_DAILY -> priceChanges.mean()
            Measurement.INDEX_REGULAR_EQUAL_GEOMETRIC_DAILY -> priceChanges.geometricMean()
            Measurement.INDEX_EXTENDED_EQUAL_ARITHMETIC_DAILY -> priceChanges.mean()
            else -> throw IllegalArgumentException("Invalid measurement: $measurement for index calculation")
        }

    private fun validateMember(ticker: String, date: LocalDate) =
        marketDataFacade.getEarliestSourceBarDataEntry(ticker)?.let { firstEntry ->
            if (firstEntry.toLocalDate() != date) {
                cacheRepository.deleteIndexStaleFrom()
                throw IllegalStateException(
                    "Date: $date is not when ticker: $ticker gets introduced, $firstEntry would be."
                )
            }
        }
}
