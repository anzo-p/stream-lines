package net.anzop.gather.model.financials

import java.time.LocalDate
import net.anzop.gather.model.financials.FinancialPeriod.ANNUAL
import net.anzop.gather.model.financials.FinancialPeriod.QUARTER1
import net.anzop.gather.model.financials.FinancialPeriod.QUARTER2

data class ReportPeriod(
    val fiscalYear: Int,
    val financialPeriod: FinancialPeriod
) {
    fun isLatest(periodType: ReportPeriodType): Boolean =
        when (periodType) {
            ReportPeriodType.ANNUAL -> this == latestAnnual()
            ReportPeriodType.QUARTER -> this == latestQuarter()
            else -> error("Invalid period type: $periodType")
        }

    @Override
    override fun toString(): String =
        "${fiscalYear}${financialPeriod.code}"

    companion object {
        fun fromString(value: String): ReportPeriod {
            check(value.substring(0, 4).matches(Regex("\\d{4}"))) { "Invalid fiscal year format: $value" }

            return ReportPeriod(
                fiscalYear = value.substring(0, 4).toInt(),
                financialPeriod = FinancialPeriod.fromCode(value.substring(4)),
            )
        }

        fun latestAnnual(): ReportPeriod =
            ReportPeriod(
                fiscalYear = LocalDate.now().minusYears(1).year,
                financialPeriod = ANNUAL,
            )

        fun latestQuarter(): ReportPeriod {
            val today = LocalDate.now()
            val month = today.monthValue

            val (year, period) = when (month) {
                in 1..3 -> Pair(today.year - 1, FinancialPeriod.QUARTER4)
                in 4..6 -> Pair(today.year, QUARTER1)
                in 7..9 -> Pair(today.year, QUARTER2)
                in 10..12 -> Pair(today.year, FinancialPeriod.QUARTER3)
                else -> error("Invalid month value: $month")
            }

            return ReportPeriod(
                fiscalYear = year,
                financialPeriod = period
            )
        }
    }
}
