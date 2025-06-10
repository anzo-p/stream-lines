package net.anzop.gather.model

data class MarketDataParams(
    val ticker: String,
    val companyName: String,
    val stockSeries: String? = null,
)

data class SourceDataParams(
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
            alpacaTicker: String,
            companyName: String,
        ) = SourceDataParams(
            marketData = MarketDataParams(
                ticker = alpacaTicker,
                companyName = companyName,
            )
        )
    }
}
