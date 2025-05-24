package net.anzop.gather.service

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import net.anzop.gather.config.AlpacaProps
import net.anzop.gather.helpers.date.generateWeekdayRange
import net.anzop.gather.helpers.date.getPreviousBankDay
import net.anzop.gather.helpers.date.minOfOptWithFallback
import net.anzop.gather.helpers.date.toInstant
import net.anzop.gather.helpers.date.toLocalDate
import net.anzop.gather.model.IndexMembers
import net.anzop.gather.model.marketData.BarData
import net.anzop.gather.model.marketData.Measurement
import net.anzop.gather.model.marketData.PriceChange
import net.anzop.gather.model.marketData.mean
import net.anzop.gather.repository.dynamodb.CacheRepository
import net.anzop.gather.repository.influxdb.MarketDataFacade
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

@Service
class IndexProcessor(
    private val alpacaProps: AlpacaProps,
    private val cacheRepository: CacheRepository,
    private val indexMemberCreator: IndexMemberCreator,
    private val marketDataFacade: MarketDataFacade,
) {
    private val logger = LoggerFactory.getLogger(IndexProcessor::class.java)

    private var asyncRecordsToInsert = mutableListOf<PriceChange>()

    fun run() =
        try {
            Measurement.indexMeasurements.forEach(::process)
            marketDataFacade.saveAsync(asyncRecordsToInsert)
        } catch (e: Exception) {
            logger.error("IndexProcessor failed", e)
        } finally {
            cacheRepository.deleteIndexStaleFrom()
            asyncRecordsToInsert.clear()
        }

    private fun process(measurement: Measurement) {
        val startDate = resolveStartDate(measurement)
        val processingPeriod = generateWeekdayRange(
            startDate = startDate,
            endDate = Instant.now().toLocalDate()
        )
        logger.info("Processing index for ${measurement.code} from $startDate for ${processingPeriod.size} days")

        val securities = cacheRepository
            .getMemberSecurities(measurement)
            .toMutableMap()

        val initialIndexValue = marketDataFacade.getIndexValueAt(measurement, startDate) ?: 1.0
        logger.info("Starting Index Value is: $initialIndexValue")

        val latestIndexValue = processPeriod(
            measurement = measurement,
            period = processingPeriod,
            securities = securities,
            initIndexValue = initialIndexValue
        )
        logger.info("Final Index Value is: $latestIndexValue")

        cacheRepository.storeMemberSecurities(measurement, securities)
    }

    // Calculating only stale and/or missing data leads to near 100% optimization on regular runs
    fun resolveStartDate(measurement: Measurement): LocalDate =
        cacheRepository
            .getIndexStaleFrom()
            ?.toInstant()
            ?.let { indexStaleFrom ->
                minOfOptWithFallback(
                    instant1 = indexStaleFrom,
                    instant2 = marketDataFacade.getLatestIndexEntry(measurement),
                    fallbackAction = { cacheRepository.deleteIndexStaleFrom() }
                )
                    ?.toLocalDate()
                    ?.getPreviousBankDay() }
            ?: alpacaProps.earliestHistoricalDate

    private fun processPeriod(
        measurement: Measurement,
        period: List<LocalDate>,
        securities: IndexMembers,
        initIndexValue: Double
    ): Double {
        val initialDay = period
            .first()
            .getPreviousBankDay()

        val initialPrices = marketDataFacade
            .getSourceBarData(
                date = initialDay,
                onlyRegularTradingHours = Measurement.regularHours(measurement)
            )
            .associate { it.ticker to it.volumeWeightedAvgPrice }

        return foldPeriod(
            measurement = measurement,
            period = period,
            securities = securities,
            initDay = initialDay,
            initPrices = initialPrices,
            initIndexValue = initIndexValue
        )
    }

    private fun foldPeriod(
        measurement: Measurement,
        period: List<LocalDate>,
        securities: IndexMembers,
        initDay: LocalDate,
        initPrices: Map<String, Double>,
        initIndexValue: Double
    ): Double =
        period.fold(initIndexValue) { currIndexValue, date ->
            if (ChronoUnit.DAYS.between(alpacaProps.earliestHistoricalDate, date) % 61 == 0L) {
                logger.info("Processing ${measurement.code}. Current date: $date, current index value: $currIndexValue")
            }

            val bars = marketDataFacade.getSourceBarData(
                date = date,
                onlyRegularTradingHours = Measurement.regularHours(measurement)
            )

            bars.forEach { bar ->
                securities.computeIfAbsent(bar.ticker) {
                    indexMemberCreator.createIndexMember(
                        bar = bar,
                        measurement = measurement,
                        inclusionDate = date,
                        indexValueAtInclusion = currIndexValue
                    )
                }

                if (date == period.first()) {
                    securities.keys.forEach { ticker ->
                        val existingMember = requireNotNull(securities[ticker]) {
                            "IndexMember entry for ticker $ticker is missing"
                        }

                        securities[ticker] = indexMemberCreator.updatePrevDay(
                            indexMember = existingMember,
                            prevDayDate = initDay,
                            prevDayAvgPrice = initPrices[ticker] ?: bar.volumeWeightedAvgPrice
                        )
                    }
                }
            }

            val priceChanges = processBars(measurement, securities, bars, date)
            asyncRecordsToInsert.addAll(priceChanges)

            resolveIndexValue(measurement, priceChanges, currIndexValue)
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

                securities[bar.ticker] = indexMemberCreator.updatePrevDay(
                    indexMember = entry,
                    prevDayDate = indexDate,
                    prevDayAvgPrice = bar.volumeWeightedAvgPrice
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
            Measurement.INDEX_EXTENDED_EQUAL_ARITHMETIC_DAILY -> priceChanges.mean()
            else -> throw IllegalArgumentException("Invalid measurement: $measurement for index calculation")
        }
}
