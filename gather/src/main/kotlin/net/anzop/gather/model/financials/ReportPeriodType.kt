package net.anzop.gather.model.financials

enum class ReportPeriodType(val code: String) {
    ANNUAL("A"),
    QUARTER("Q");

    override fun toString(): String = code
}
