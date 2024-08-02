package net.anzop.retro.model.marketData

enum class Measurement(val code: String, val description: String) {
    SECURITY_RAW_SEMI_HOURLY(
        "sec_raw_30mi",
        "Market data as 30 minute bars for individual securities"
    ),
    SECURITY_REGULAR_PRICE_CHANGE_ARITHMETIC_DAILY(
        "sec_reg_arith_d",
        "Current prices compared to introduction day prices during regular trading hours " +
        "of a security as input to an equally wighted index using an arithmetic mean"
    ),
    SECURITY_REGULAR_PRICE_CHANGE_GEOMETRIC_DAILY(
        "sec_reg_geo_d",
        "Current prices compared to introduction day prices during regular trading hours " +
        "of a security as input to an equally wighted index using a geometric mean"
    ),
    INDEX_REGULAR_EQUAL_ARITHMETIC_DAILY(
        "ix_reg_arith_d",
        "Index using arithmetic mean from daily regular trading hours data"
    ),
    INDEX_REGULAR_EQUAL_GEOMETRIC_DAILY(
        "ix_reg_geo_d",
        "Index using geometric mean from daily regular trading hours data"
    );

    companion object {
        private val map = entries.associateBy(Measurement::code)

        fun fromCode(code: String): Measurement {
            return map[code] ?: throw IllegalArgumentException("Invalid code: $code for enum Measurement")
        }

        fun indexMeasurements() = entries.filter { it.code.startsWith("ix_") }

        fun securitiesForIndex(index: Measurement) = when (index) {
            INDEX_REGULAR_EQUAL_ARITHMETIC_DAILY -> SECURITY_REGULAR_PRICE_CHANGE_ARITHMETIC_DAILY
            INDEX_REGULAR_EQUAL_GEOMETRIC_DAILY -> SECURITY_REGULAR_PRICE_CHANGE_GEOMETRIC_DAILY
            else -> throw IllegalArgumentException("Measurement: $index does not have assigned securities measurement")
        }
    }
}
