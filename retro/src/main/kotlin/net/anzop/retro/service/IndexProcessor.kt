package net.anzop.retro.service

import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import net.anzop.retro.config.AlpacaProps
import net.anzop.retro.helpers.date.generateWeekdayRange
import net.anzop.retro.helpers.date.toInstant
import net.anzop.retro.helpers.date.toLocalDate
import net.anzop.retro.model.IndexMember
import net.anzop.retro.model.marketData.BarData
import net.anzop.retro.model.marketData.Measurement
import net.anzop.retro.model.marketData.PriceChange
import net.anzop.retro.model.marketData.div
import net.anzop.retro.model.marketData.plus
import net.anzop.retro.repository.dynamodb.CacheRepository
import net.anzop.retro.repository.influxdb.MarketDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private typealias IndexMembers = MutableMap<String, IndexMember>

@Service
class IndexProcessor(
    private val alpacaProps: AlpacaProps,
    private val cacheRepository: CacheRepository,
    private val marketDataRepository: MarketDataRepository,
) {
    private val logger = LoggerFactory.getLogger(IndexProcessor::class.java)

    private var asyncRecordsToInsert = mutableListOf<Any>()

    fun process() {
        val startDate = cacheRepository.getIndexStaleFrom()?.minusDays(1)
            ?: run {
                cacheRepository.deleteMemberSecurities()
                alpacaProps.earliestHistoricalDate
            }
        logger.info("Processing index from $startDate")

        val initialIndexValue = marketDataRepository.getIndexValueAt(startDate) ?: 1.0
        logger.info("Starting Index Value is: $initialIndexValue")

        val processingPeriod = generateWeekdayRange(
            startDate = startDate,
            endDate = Instant.now().toLocalDate()
        )
        logger.info("${processingPeriod.size} days to process the index for")

        val latestIndexValue = loop(processingPeriod, initialIndexValue)
        logger.info("Final Index Value is: $latestIndexValue")
    }

    fun loop(period: List<LocalDate>, initialIndexValue: Double): Double {
        val securities = cacheRepository.getMemberSecurities().toMutableMap()

        val latestIndexValue = period.fold(initialIndexValue) { currIndexValue, date ->
            if (ChronoUnit.DAYS.between(alpacaProps.earliestHistoricalDate, date) % 61 == 0L) {
                logger.info("Processing. Current date: $date, current index value: $currIndexValue")
            }

            val bars = marketDataRepository.getSourceBarData(
                date = date,
                onlyRegularTradingHours = true
            )
            bars.forEach { bar ->
                securities.computeIfAbsent(bar.ticker) {
                    validateMember(bar.ticker, date)
                    IndexMember(
                        indexValueWhenIntroduced = currIndexValue,
                        introductionPrice = bar.volumeWeightedAvgPrice,
                        prevDayPrice = bar.volumeWeightedAvgPrice,
                        ticker = bar.ticker
                    )
                }
            }

            val priceChanges = processBars(securities, bars, date)
            asyncRecordsToInsert.addAll(priceChanges)
            resolveNewIndexValue(priceChanges, currIndexValue)
        }

        marketDataRepository.saveAsync(asyncRecordsToInsert)
        asyncRecordsToInsert.clear()

        cacheRepository.storeMemberSecurities(securities)
        cacheRepository.deleteIndexStaleFrom()

        return latestIndexValue
    }

    private fun processBars(securities: IndexMembers, bars: List<BarData>, indexDate: LocalDate): List<PriceChange> =
        bars.mapNotNull { bar ->
            securities[bar.ticker]?.let { entry ->
                val (indexValueWhenIntroduced, introductionPrice, prevDayPrice) = entry

                fun normalize(price: Double): Double =
                    (price / introductionPrice) * indexValueWhenIntroduced

                securities[bar.ticker] = entry.copy(prevDayPrice = bar.volumeWeightedAvgPrice)
                val priceChangeAvg = normalize(bar.volumeWeightedAvgPrice)

                PriceChange(
                    measurement = Measurement.SECURITIES_WEIGHTED_EQUAL_DAILY,
                    company = bar.company,
                    ticker = bar.ticker,
                    regularTradingHours = bar.regularTradingHours,
                    marketTimestamp = indexDate.toInstant(),
                    priceChangeOpen = normalize(bar.openingPrice),
                    priceChangeClose = normalize(bar.closingPrice),
                    priceChangeHigh = normalize(bar.highPrice),
                    priceChangeLow = normalize(bar.lowPrice),
                    priceChangeAvg = priceChangeAvg,
                    priceChangeDaily = priceChangeAvg / normalize(prevDayPrice),
                    totalTradingValue = bar.totalTradingValue
                )
            }
        }

    private fun resolveNewIndexValue(priceChanges: List<PriceChange>, indexValue: Double): Double =
        priceChanges.takeIf { it.isNotEmpty() }
            ?.let { createIndex(it) }
            ?.priceChangeAvg
            ?: indexValue

    private fun createIndex(priceChanges: List<PriceChange>): PriceChange {
        val indexBar = priceChanges
            .reduce { acc, priceChange -> acc + priceChange }
            .copy(
                measurement = Measurement.INDEX_WEIGHTED_EQUAL_DAILY,
                company = "INDEX",
                ticker = "INDEX"
            )
            .div(priceChanges.size.toDouble())

        marketDataRepository.save(indexBar)

        return indexBar
    }

    private fun validateMember(ticker: String, date: LocalDate) =
        marketDataRepository.getEarliestSourceBarDataEntry(ticker)?.let { firstEntry ->
            if (firstEntry.toLocalDate() != date) {
                cacheRepository.deleteIndexStaleFrom()
                throw IllegalStateException(
                    "Date: $date is not when ticker: $ticker gets introduced, $firstEntry would be."
                )
            }
        }
}
