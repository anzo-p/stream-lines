package net.anzop.retro.service

import java.time.Instant
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import net.anzop.retro.config.AlpacaProps
import net.anzop.retro.helpers.genWeekdayRange
import net.anzop.retro.model.BarData
import net.anzop.retro.model.Measurement
import net.anzop.retro.model.plus
import net.anzop.retro.model.ratio
import net.anzop.retro.repository.BarDataRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

private data class MemberSecurity (
    val initPrice: Double,
    val indexValueWhenIntroduced: Double
)

private typealias Securities = Map<String, MemberSecurity>

@Service
class IndexProcessor(
    private val alpacaProps: AlpacaProps,
    private val barDataRepository: BarDataRepository,
) {
    private val logger = LoggerFactory.getLogger(BarDataFetcher::class.java)

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

            bars.forEach { barData ->
                securities.computeIfAbsent(barData.ticker) {
                    MemberSecurity(
                        initPrice = barData.volumeWeightedAvgPrice,
                        indexValueWhenIntroduced = currIndexValue
                    )
                }
            }

            val weightedBars = processBars(securities, bars)
            weightedBars.forEach(barDataRepository::save)

            resolveNewIndexValue(weightedBars, currIndexValue)
        }

        logger.info("Final Index Value is: $latestIndexValue")
    }

    private fun processBars(securities: Securities, bars: List<BarData>): List<BarData> =
        bars.mapNotNull { barData ->
            securities[barData.ticker]?.let { (initPrice, indexValueWhenIntroduced) ->
                fun normalize(from: Double, to: Double): Double =
                    (to / from) * indexValueWhenIntroduced

                BarData(
                    measurement = Measurement.SECURITIES_WEIGHTED_EQUAL_DAILY,
                    ticker = barData.ticker,
                    openingPrice = normalize(initPrice, barData.openingPrice),
                    closingPrice = normalize(initPrice, barData.closingPrice),
                    highPrice = normalize(initPrice, barData.highPrice),
                    lowPrice = normalize(initPrice, barData.lowPrice),
                    volumeWeightedAvgPrice = normalize(initPrice, barData.volumeWeightedAvgPrice),
                    totalTradingValue = barData.totalTradingValue,
                    marketTimestamp = barData.marketTimestamp,
                )
            }
        }

    private fun resolveNewIndexValue(bars: List<BarData>, indexValue: Double): Double =
        bars.takeIf { it.isNotEmpty() }
            ?.let { createIndex(it) }
            ?.volumeWeightedAvgPrice
            ?: indexValue

    private fun createIndex(bars: List<BarData>): BarData {
        val indexBar = bars
            .reduce { acc, barData -> acc + barData }
            .copy(
                measurement = Measurement.INDEX_WEIGHTED_EQUAL_DAILY,
                ticker = "INDEX"
            )
            .ratio(bars.size)

        barDataRepository.save(indexBar)

        return indexBar
    }
}
