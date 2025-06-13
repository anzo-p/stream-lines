package net.anzop.gather.repository.dynamodb

import net.anzop.gather.config.AwsConfig
import net.anzop.gather.config.SourceDataConfig
import net.anzop.gather.model.financials.FinancialPeriod
import net.anzop.gather.model.financials.Financials
import net.anzop.gather.model.financials.ReportPeriod
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

@Repository
class FinancialsRepository(
    awsConfig: AwsConfig,
    dynamoDbClient: DynamoDbClient,
) : DynamoDbCore(awsConfig, dynamoDbClient) {

    fun storeFinancials(ticker: String, financials: List<Financials>) =
        financials.map {
            saveItem(
                mapOf(
                    "PK" to "financials#$ticker".toAttrib(),
                    "SK" to it.reportPeriod.toString().toAttrib(),
                    "financialData" to it.toAttrib()
                )
            )
        }


    fun queryFinancials(ticker: String): List<Financials> =
        queryItems("financials#$ticker")
            .takeIf { it.isNotEmpty() }
            ?.map { parseFinancials(it) }
            ?.flatten()
            .orEmpty()

    fun queryReportPeriods(ticker: String): List<ReportPeriod> =
        queryItems("financials#$ticker")
            .takeIf { it.isNotEmpty() }
            ?.map {
                it["SK"]?.toStringOrNull()?.let { ReportPeriod.fromString(it) }
            }
            ?.filterNotNull()
            .orEmpty()

    fun getFinancials(ticker: String, reportPeriod: ReportPeriod): Financials? {
        val key = mapOf(
            "PK" to "financials#$ticker".toAttrib(),
            "SK" to reportPeriod.toString().toAttrib(),
        )

        return getItem(key)
            ?.let { parseFinancials(it).firstOrNull() }
    }

    private fun Financials.toAttrib(): AttributeValue =
        AttributeValue.builder().m(
            mapOf(
                "ticker" to this.params.marketData.ticker.toAttrib(),
                "company" to this.params.marketData.companyName.toAttrib(),
                "fiscalYear" to this.reportPeriod.fiscalYear.toAttrib(),
                "annualOrQuarter" to this.reportPeriod.financialPeriod.toString().toAttrib(),
                "currency" to this.currency.toAttrib(),
                "revenue" to this.revenue?.toAttrib(),
                "costOfRevenue" to this.costOfRevenue?.toAttrib(),
                "grossProfit" to this.grossProfit?.toAttrib(),
                "operatingIncome" to this.operatingIncome?.toAttrib(),
                "totalAssets" to this.totalAssets?.toAttrib(),
                "totalCurrentAssets" to this.totalCurrentAssets?.toAttrib(),
                "prepaidExpenses" to this.prepaidExpenses?.toAttrib(),
                "propertyPlantAndEquipmentNet" to this.propertyPlantAndEquipmentNet?.toAttrib(),
                "retainedEarnings" to this.retainedEarnings?.toAttrib(),
                "otherAssetsNonCurrent" to this.otherAssetsNonCurrent?.toAttrib(),
                "totalNonCurrentAssets" to this.totalNonCurrentAssets?.toAttrib(),
                "totalLiabilities" to this.totalLiabilities?.toAttrib(),
                "shareholderEquity" to this.shareholderEquity?.toAttrib(),
                "netIncome" to this.netIncome?.toAttrib(),
                "sharesOutstandingDiluted" to this.sharesOutstandingDiluted?.toAttrib(),
                "sharesOutstandingBasic" to this.sharesOutstandingBasic?.toAttrib(),
                "epsDiluted" to this.epsDiluted?.toAttrib(),
                "epsBasic" to this.epsBasic?.toAttrib(),
                "operatingCashFlow" to this.operatingCashFlow?.toAttrib(),
                "investingCashFlow" to this.investingCashFlow?.toAttrib(),
                "financingCashFlow" to this.financingCashFlow?.toAttrib(),
                "netCashFlow" to this.netCashFlow?.toAttrib(),
                "researchDevelopmentExpense" to this.researchDevelopmentExpense?.toAttrib(),
                "sellingGeneralAdministrativeExpense" to this.sellingGeneralAdministrativeExpense?.toAttrib(),
                "operatingExpenses" to this.operatingExpenses?.toAttrib(),
                "nonOperatingIncome" to this.nonOperatingIncome?.toAttrib(),
                "preTaxIncome" to this.preTaxIncome?.toAttrib(),
                "incomeTax" to this.incomeTax?.toAttrib(),
                "depreciationAmortization" to this.depreciationAmortization?.toAttrib(),
                "stockBasedCompensation" to this.stockBasedCompensation?.toAttrib(),
                "dividendsPaid" to this.dividendsPaid?.toAttrib(),
                "cashOnHand" to this.cashOnHand?.toAttrib(),
                "currentNetReceivables" to this.currentNetReceivables?.toAttrib(),
                "inventory" to this.inventory?.toAttrib(),
                "totalCurrentLiabilities" to this.totalCurrentLiabilities?.toAttrib(),
                "totalNonCurrentLiabilities" to this.totalNonCurrentLiabilities?.toAttrib(),
                "longTermDebt" to this.longTermDebt?.toAttrib(),
                "totalLongTermLiabilities" to this.totalLongTermLiabilities?.toAttrib(),
                "goodwill" to this.goodwill?.toAttrib(),
                "intangibleAssetsExcludingGoodwill" to this.intangibleAssetsExcludingGoodwill?.toAttrib()
            )
        ).build()

    private fun parseFinancials(attribs: AttribMap): List<Financials> =
        attribs["financialData"]
            ?.takeIf { it.hasM() }
            ?.m()
            ?.let { listOfNotNull(attribToFinancials(it)) }
            ?: emptyList()

    private fun attribToFinancials(attribs: Map<String, AttributeValue>): Financials? =
        attribs.takeIf { it.isNotEmpty() }?.let {
            attribs["ticker"]?.toStringOrError("Ticker not found")?.let { ticker ->
                val params = SourceDataConfig
                    .resolve(ticker)
                    ?: error("Ticker $ticker not found in SourceDataConfig")

                Financials(
                    params = params,
                    reportPeriod = ReportPeriod(
                        fiscalYear = attribs["fiscalYear"].toIntOrError("Fiscal year not found"),
                        financialPeriod = FinancialPeriod.fromCode(
                            attribs["annualOrQuarter"].toStringOrError("Financial period not found")
                        )
                    ),
                    currency = attribs["currency"].toStringOrDefault(),
                    revenue = attribs["revenue"].toLongOrNull(),
                    costOfRevenue = attribs["costOfRevenue"].toLongOrNull(),
                    grossProfit = attribs["grossProfit"].toLongOrNull(),
                    operatingIncome = attribs["operatingIncome"].toLongOrNull(),
                    totalAssets = attribs["totalAssets"].toLongOrNull(),
                    totalCurrentAssets = attribs["totalCurrentAssets"].toLongOrNull(),
                    prepaidExpenses = attribs["prepaidExpenses"].toLongOrNull(),
                    propertyPlantAndEquipmentNet = attribs["propertyPlantAndEquipmentNet"].toLongOrNull(),
                    retainedEarnings = attribs["retainedEarnings"].toLongOrNull(),
                    otherAssetsNonCurrent = attribs["otherAssetsNonCurrent"].toLongOrNull(),
                    totalNonCurrentAssets = attribs["totalNonCurrentAssets"].toLongOrNull(),
                    totalLiabilities = attribs["totalLiabilities"].toLongOrNull(),
                    shareholderEquity = attribs["shareholderEquity"].toLongOrNull(),
                    netIncome = attribs["netIncome"].toLongOrNull(),
                    sharesOutstandingDiluted = attribs["sharesOutstandingDiluted"].toLongOrNull(),
                    sharesOutstandingBasic = attribs["sharesOutstandingBasic"].toLongOrNull(),
                    epsDiluted = attribs["epsDiluted"].toDoubleOrNull(),
                    epsBasic = attribs["epsBasic"].toDoubleOrNull(),
                    operatingCashFlow = attribs["operatingCashFlow"].toLongOrNull(),
                    investingCashFlow = attribs["investingCashFlow"].toLongOrNull(),
                    financingCashFlow = attribs["financingCashFlow"].toLongOrNull(),
                    netCashFlow = attribs["netCashFlow"].toLongOrNull(),
                    researchDevelopmentExpense = attribs["researchDevelopmentExpense"].toLongOrNull(),
                    sellingGeneralAdministrativeExpense = attribs["sellingGeneralAdministrativeExpense"].toLongOrNull(),
                    operatingExpenses = attribs["operatingExpenses"].toLongOrNull(),
                    nonOperatingIncome = attribs["nonOperatingIncome"].toLongOrNull(),
                    preTaxIncome = attribs["preTaxIncome"].toLongOrNull(),
                    incomeTax = attribs["incomeTax"].toLongOrNull(),
                    depreciationAmortization = attribs["depreciationAmortization"].toLongOrNull(),
                    stockBasedCompensation = attribs["stockBasedCompensation"].toLongOrNull(),
                    dividendsPaid = attribs["dividendsPaid"].toLongOrNull(),
                    cashOnHand = attribs["cashOnHand"].toLongOrNull(),
                    currentNetReceivables = attribs["currentNetReceivables"].toLongOrNull(),
                    inventory = attribs["inventory"].toLongOrNull(),
                    totalCurrentLiabilities = attribs["totalCurrentLiabilities"].toLongOrNull(),
                    totalNonCurrentLiabilities = attribs["totalNonCurrentLiabilities"].toLongOrNull(),
                    longTermDebt = attribs["longTermDebt"].toLongOrNull(),
                    totalLongTermLiabilities = attribs["totalLongTermLiabilities"].toLongOrNull(),
                    goodwill = attribs["goodwill"].toLongOrNull(),
                    intangibleAssetsExcludingGoodwill = attribs["intangibleAssetsExcludingGoodwill"].toLongOrNull(),
                )
            }
        }
}
