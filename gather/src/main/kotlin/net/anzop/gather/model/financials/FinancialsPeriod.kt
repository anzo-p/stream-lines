package net.anzop.gather.model.financials

enum class FinancialPeriod(val code: String) {
    ANNUAL("A"),
    QUARTER1("Q1"),
    QUARTER2("Q2"),
    QUARTER3("Q3"),
    QUARTER4("Q4");

    companion object {
        fun fromCode(code: String): FinancialPeriod =
            entries.associateBy { it.code }[code] ?: error("Unknown financial period code: $code")
    }
}
