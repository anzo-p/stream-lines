package net.anzop.retro.model

data class Ticker(
    val symbol: String,
    val company: String,
    val series: String? = null
) {
    init {
        require(symbol.matches(Regex("^[A-Z.]{1,5}\$"))) {
            "Invalid symbol: $symbol. Must have up to 5 uppercase letters. Periods also allowed."
        }

        require(company.isNotBlank()) { "Company name must not be blank." }
    }
}
