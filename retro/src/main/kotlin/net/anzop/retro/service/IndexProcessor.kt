package net.anzop.retro.service

import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import net.anzop.retro.config.AlpacaProps
import net.anzop.retro.helpers.genWeekdayRange
import net.anzop.retro.model.BarData
import net.anzop.retro.model.Measurement
import net.anzop.retro.model.PriceChangeWeighted
import net.anzop.retro.model.div
import net.anzop.retro.model.plus
import net.anzop.retro.repository.BarDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private data class MemberSecurity (
    val indexValueWhenIntroduced: Double,
    val introductionPrice: Double,
    val prevDayPrice: Double
)

private typealias Securities = MutableMap<String, MemberSecurity>

@Service
class IndexProcessor(
    private val alpacaProps: AlpacaProps,
    private val barDataRepository: BarDataRepository,
) {
    private val logger = LoggerFactory.getLogger(IndexProcessor::class.java)

    fun process() {
        val securities = mutableMapOf<String, MemberSecurity>()
        val processingPeriod = genWeekdayRange(
            startDate = alpacaProps.earliestHistoricalDate,
            endDate = Instant.now().atZone(ZoneOffset.UTC).toLocalDate()
        )
        logger.info("${processingPeriod.size} days to process the index for")

        val latestIndexValue = processingPeriod.fold(1.0) { currIndexValue, date ->
            if (ChronoUnit.DAYS.between(alpacaProps.earliestHistoricalDate, date) % 61 == 0L) {
                logger.info("Processing. Current date: $date, current index value: $currIndexValue")
            }

            val bars = barDataRepository.getMeasurements(
                measurement = Measurement.SECURITIES_RAW_DAILY,
                from = date.atStartOfDay().toInstant(ZoneOffset.UTC)
            )

            bars.forEach { bar ->
                securities.computeIfAbsent(bar.ticker) {
                    MemberSecurity(
                        indexValueWhenIntroduced = currIndexValue,
                        introductionPrice = bar.volumeWeightedAvgPrice,
                        prevDayPrice = bar.volumeWeightedAvgPrice
                    )
                }
            }

            val priceChanges = processBars(securities, bars)
            barDataRepository.saveAsync(priceChanges)

            resolveNewIndexValue(priceChanges, currIndexValue)
        }

        logger.info("Final Index Value is: $latestIndexValue")
    }

    private fun processBars(securities: Securities, bars: List<BarData>): List<PriceChangeWeighted> =
        bars.mapNotNull { bar ->
            securities[bar.ticker]?.let { entry ->
                val (indexValueWhenIntroduced, introductionPrice, prevDayPrice) = entry

                fun normalize(price: Double): Double =
                    (price / introductionPrice) * indexValueWhenIntroduced

                securities[bar.ticker] = entry.copy(prevDayPrice = bar.volumeWeightedAvgPrice)
                val priceChangeAvg = normalize(bar.volumeWeightedAvgPrice)

                PriceChangeWeighted(
                    measurement = Measurement.SECURITIES_WEIGHTED_EQUAL_DAILY,
                    ticker = bar.ticker,
                    marketTimestamp = bar.marketTimestamp,
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

    private fun resolveNewIndexValue(priceChanges: List<PriceChangeWeighted>, indexValue: Double): Double =
        priceChanges.takeIf { it.isNotEmpty() }
            ?.let { createIndex(it) }
            ?.priceChangeAvg
            ?: indexValue

    private fun createIndex(priceChanges: List<PriceChangeWeighted>): PriceChangeWeighted {
        val indexBar = priceChanges
            .reduce { acc, priceChange -> acc + priceChange }
            .copy(
                measurement = Measurement.INDEX_WEIGHTED_EQUAL_DAILY,
                ticker = "INDEX"
            )
            .div(priceChanges.size.toDouble())

        barDataRepository.save(indexBar)

        return indexBar
    }
}
