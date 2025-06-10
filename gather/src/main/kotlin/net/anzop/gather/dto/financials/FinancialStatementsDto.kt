package net.anzop.gather.dto.financials

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class FinancialStatementsDto(

    val revenue: Map<String, Double>,

    @SerialName("cost_of_revenue")
    val costOfRevenue: Map<String, Double>,

    @SerialName("gross_profit")
    val grossProfit: Map<String, Double>,

    @SerialName("operating_income")
    val operatingIncome: Map<String, Double>,

    @SerialName("total_assets")
    val totalAssets: Map<String, Double>,

    @SerialName("total_current_assets")
    val totalCurrentAssets: Map<String, Double>,

    @SerialName("prepaid_expenses")
    val prepaidExpenses: Map<String, Double>,

    @SerialName("property_plant_and_equipment_net")
    val propertyPlantAndEquipmentNet: Map<String, Double>,

    @SerialName("retained_earnings")
    val retainedEarnings: Map<String, Double>,

    @SerialName("other_assets_noncurrent")
    val otherAssetsNonCurrent: Map<String, Double>,

    // can be missing from quarterly
    @SerialName("total_non_current_assets")
    val totalNonCurrentAssets: Map<String, Double>? = null,

    @SerialName("total_liabilities")
    val totalLiabilities: Map<String, Double>,

    @SerialName("shareholder_equity")
    val shareholderEquity: Map<String, Double>,

    @SerialName("net_income")
    val netIncome: Map<String, Double>,

    @SerialName("shares_outstanding_diluted")
    val sharesOutstandingDiluted: Map<String, Double>,

    @SerialName("shares_outstanding_basic")
    val sharesOutstandingBasic: Map<String, Double>,

    @SerialName("eps_diluted")
    val epsDiluted: Map<String, Double>,

    @SerialName("eps_basic")
    val epsBasic: Map<String, Double>,

    @SerialName("operating_cash_flow")
    val operatingCashFlow: Map<String, Double>,

    @SerialName("investing_cash_flow")
    val investingCashFlow: Map<String, Double>,

    @SerialName("financing_cash_flow")
    val financingCashFlow: Map<String, Double>,

    // can be missing from quarterly
    @SerialName("net_cash_flow")
    val netCashFlow: Map<String, Double>? = null,

    @SerialName("research_development_expense")
    val researchDevelopmentExpense: Map<String, Double>,

    @SerialName("selling_general_administrative_expense")
    val sellingGeneralAdministrativeExpense: Map<String, Double>,

    @SerialName("operating_expenses")
    val operatingExpenses: Map<String, Double>,

    @SerialName("non_operating_income")
    val nonOperatingIncome: Map<String, Double>,

    @SerialName("pre_tax_income")
    val preTaxIncome: Map<String, Double>,

    @SerialName("income_tax")
    val incomeTax: Map<String, Double>,

    @SerialName("depreciation_amortization")
    val depreciationAmortization: Map<String, Double>,

    @SerialName("stock_based_compensation")
    val stockBasedCompensation: Map<String, Double>,

    @SerialName("dividends_paid")
    val dividendsPaid: Map<String, Double>,

    @SerialName("cash_on_hand")
    val cashOnHand: Map<String, Double>,

    @SerialName("current_net_receivables")
    val currentNetReceivables: Map<String, Double>,

    val inventory: Map<String, Double>,

    @SerialName("total_current_liabilities")
    val totalCurrentLiabilities: Map<String, Double>,

    @SerialName("total_non_current_liabilities")
    val totalNonCurrentLiabilities: Map<String, Double>,

    @SerialName("long_term_debt")
    val longTermDebt: Map<String, Double>,

    // can be missing from quarterly
    @SerialName("total_long_term_liabilities")
    val totalLongTermLiabilities: Map<String, Double>? = null,

    val goodwill: Map<String, Double>,

    @SerialName("intangible_assets_excluding_goodwill")
    val intangibleAssetsExcludingGoodwill: Map<String, Double>,
)
