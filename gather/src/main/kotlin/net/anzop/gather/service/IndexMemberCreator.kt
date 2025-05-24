package net.anzop.gather.service

import java.time.LocalDate
import net.anzop.gather.helpers.date.toLocalDate
import net.anzop.gather.model.IndexMember
import net.anzop.gather.model.PrevDayData
import net.anzop.gather.model.marketData.BarData
import net.anzop.gather.model.marketData.Measurement
import net.anzop.gather.repository.influxdb.MarketDataFacade
import org.springframework.stereotype.Component

@Component
class IndexMemberCreator(
    private val marketDataFacade: MarketDataFacade
) {
    fun createIndexMember(
        bar: BarData,
        measurement: Measurement,
        inclusionDate: LocalDate,
        indexValueAtInclusion: Double
    ): IndexMember =
        bar
            .also { validateIndexMember(it.ticker, inclusionDate) }
            .let {
                IndexMember(
                    ticker = it.ticker,
                    measurement = measurement,
                    indexValueWhenIntroduced = indexValueAtInclusion,
                    introductionPrice = it.volumeWeightedAvgPrice,
                    prevDayData = PrevDayData(
                        date = inclusionDate,
                        avgPrice = it.volumeWeightedAvgPrice
                    )
                )
            }

    fun updatePrevDay(
        indexMember: IndexMember,
        prevDayDate: LocalDate,
        prevDayAvgPrice: Double
    ): IndexMember =
        indexMember.copy(
            prevDayData = PrevDayData(
                date = prevDayDate,
                avgPrice = prevDayAvgPrice
            )
        )

    private fun validateIndexMember(ticker: String, date: LocalDate) =
        marketDataFacade
            .getEarliestSourceBarDataEntry(ticker)
            ?.toLocalDate()
            ?.let { firstEntry ->
                require(firstEntry == date) {
                    "Date: $date is before ticker: $ticker gets introduced, $firstEntry would be."
                }
            }
}
