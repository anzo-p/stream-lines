package net.anzop.gather.dto.financials

import jakarta.validation.constraints.NotNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.anzop.gather.config.SourceDataConfig
import net.anzop.gather.helpers.date.generateFinancialPeriodRange
import net.anzop.gather.model.financials.FinancialPeriod
import net.anzop.gather.model.financials.Financials
import net.anzop.gather.model.financials.ReportPeriod
import net.anzop.gather.model.financials.ReportPeriodType
import org.springframework.validation.annotation.Validated

@Serializable
@Validated
data class FinancialResponseDto(

    @field:NotNull(message = "cannot be null")
    val currency: String,

    @SerialName("company_info")
    @field:NotNull(message = "cannot be null")
    val companyInfo: CompanyInfoDto,

    @SerialName("financial_data")
    @field:NotNull(message = "cannot be null")
    val financialData: FinancialDataDto,
) {

    fun toModel(periodType: ReportPeriodType): List<Financials> =
        generateFinancialPeriodRange(periodType)
            .map { (year, financialPeriod) ->
                val financials = financialData.annualData?.takeIf { financialPeriod == FinancialPeriod.ANNUAL }
                    ?: financialData.quarterlyData
                    ?: error("Financial must either contain fiscal or quarterly data")

                val key = toFinancialStatementMapKey(year, financialPeriod)

                val params = SourceDataConfig
                    .resolve(companyInfo.ticker)
                    ?: error("Ticker ${companyInfo.ticker} not found in SourceDataConfig")

                Financials(
                    params = params,
                    reportPeriod = ReportPeriod(
                        fiscalYear = year,
                        financialPeriod = financialPeriod,
                    ),
                    currency = currency,
                    revenue = financials.revenue[key]?.toLong(),
                    costOfRevenue = financials.costOfRevenue[key]?.toLong(),
                    grossProfit = financials.grossProfit[key]?.toLong(),
                    operatingIncome = financials.operatingIncome[key]?.toLong(),
                    totalAssets = financials.totalAssets[key]?.toLong(),
                    totalCurrentAssets = financials.totalCurrentAssets[key]?.toLong(),
                    prepaidExpenses = financials.prepaidExpenses[key]?.toLong(),
                    propertyPlantAndEquipmentNet = financials.propertyPlantAndEquipmentNet[key]?.toLong(),
                    retainedEarnings = financials.retainedEarnings[key]?.toLong(),
                    otherAssetsNonCurrent = financials.otherAssetsNonCurrent[key]?.toLong(),
                    totalNonCurrentAssets = financials.totalNonCurrentAssets?.get(key)?.toLong(),
                    totalLiabilities = financials.totalLiabilities[key]?.toLong(),
                    shareholderEquity = financials.shareholderEquity[key]?.toLong(),
                    netIncome = financials.netIncome[key]?.toLong(),
                    sharesOutstandingDiluted = financials.sharesOutstandingDiluted[key]?.toLong(),
                    sharesOutstandingBasic = financials.sharesOutstandingBasic[key]?.toLong(),
                    epsDiluted = financials.epsDiluted[key],
                    epsBasic = financials.epsBasic[key],
                    operatingCashFlow = financials.operatingCashFlow[key]?.toLong(),
                    investingCashFlow = financials.investingCashFlow[key]?.toLong(),
                    financingCashFlow = financials.financingCashFlow[key]?.toLong(),
                    netCashFlow = financials.netCashFlow?.get(key)?.toLong(),
                    researchDevelopmentExpense = financials.researchDevelopmentExpense[key]?.toLong(),
                    sellingGeneralAdministrativeExpense = financials.sellingGeneralAdministrativeExpense[key]?.toLong(),
                    operatingExpenses = financials.operatingExpenses[key]?.toLong(),
                    nonOperatingIncome = financials.nonOperatingIncome[key]?.toLong(),
                    preTaxIncome = financials.preTaxIncome[key]?.toLong(),
                    incomeTax = financials.incomeTax[key]?.toLong(),
                    depreciationAmortization = financials.depreciationAmortization[key]?.toLong(),
                    stockBasedCompensation = financials.stockBasedCompensation[key]?.toLong(),
                    dividendsPaid = financials.dividendsPaid[key]?.toLong(),
                    cashOnHand = financials.cashOnHand[key]?.toLong(),
                    currentNetReceivables = financials.currentNetReceivables[key]?.toLong(),
                    inventory = financials.inventory[key]?.toLong(),
                    totalCurrentLiabilities = financials.totalCurrentLiabilities[key]?.toLong(),
                    totalNonCurrentLiabilities = financials.totalNonCurrentLiabilities[key]?.toLong(),
                    longTermDebt = financials.longTermDebt[key]?.toLong(),
                    totalLongTermLiabilities = financials.totalLongTermLiabilities?.get(key)?.toLong(),
                    goodwill = financials.goodwill[key]?.toLong(),
                    intangibleAssetsExcludingGoodwill = financials.intangibleAssetsExcludingGoodwill[key]?.toLong(),
                )
            }

    private fun toFinancialStatementMapKey(
        year: Int,
        financialPeriod: FinancialPeriod,
    ): String =
        when (financialPeriod) {
            FinancialPeriod.ANNUAL -> "$year"
            FinancialPeriod.QUARTER1 -> "$year-Q1"
            FinancialPeriod.QUARTER2 -> "$year-Q2"
            FinancialPeriod.QUARTER3 -> "$year-Q3"
            FinancialPeriod.QUARTER4 -> "$year-Q4"
        }
}
