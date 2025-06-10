package net.anzop.gather.model

import net.anzop.gather.helpers.StringHelpers

data class FundamentalsParams(
    val ticker: String? = null,
    val skip: Boolean = false,
)

data class MarketDataParams(
    val ticker: String,
    val companyName: String,
    val stockSeries: String? = null,
)

data class SourceDataParams(
    val fundamentals: FundamentalsParams? = null,
    val marketData: MarketDataParams,
) {
    init {
        require(marketData.ticker.matches(Regex("^[A-Z.]{1,5}\$"))) {
            "Invalid symbol: $marketData.ticker. Must have up to 5 uppercase letters. Periods also allowed."
        }

        require(marketData.companyName.isNotBlank()) { "Company name must not be blank." }
    }

    companion object {
        fun create(
            alpacaTicker: String? = null,
            dataJockeyTicker: String? = null,
            companyName: String,
        ) = SourceDataParams(
            fundamentals = FundamentalsParams(
                ticker = dataJockeyTicker
            ),
            marketData = MarketDataParams(
                ticker = StringHelpers.xorNotNull(
                    alpacaTicker,
                    dataJockeyTicker?.replace("-", ""),
                    "Either Alpaca ticker or Data Jockey ticker must be provided."
                ),
                companyName = companyName
            )
        )
    }
}
