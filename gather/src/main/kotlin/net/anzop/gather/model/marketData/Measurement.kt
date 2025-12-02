package net.anzop.gather.model.marketData

enum class Measurement(val code: String, val description: String) {
    SECURITIES_DAILY_BARS_RAW(
        "securities-daily-bars-raw",
        "Market data one day bars for individual securities"
    ),
    SECURITIES_DAILY_CHANGE_REGULAR_HOURS(
        "securities-daily-change-regular-hours",
        "Current prices compared to introduction day prices during regular trading hours " +
        "of a security as input to an equally wighted index using an arithmetic mean"
    ),
    SECURITIES_DAILY_CHANGE_EXTENDED_HOURS(
        "securities-daily-change-extended-hours",
        "Current prices compared to introduction day prices including extended trading hours " +
        "of a security as input to an equally wighted index using an arithmetic mean"
    ),
    INDEX_DAILY_CHANGE_REGULAR_HOURS(
        "index-daily-change-regular-hours",
        "Index using arithmetic mean from daily regular trading hours data"
    ),
    INDEX_DAILY_CHANGE_EXTENDED_HOURS(
        "index-daily-change-extended-hours",
        "Index using arithmetic mean from daily data including extended hours"
    );

    fun regularHours() =
        when (this) {
            INDEX_DAILY_CHANGE_EXTENDED_HOURS -> true
            INDEX_DAILY_CHANGE_REGULAR_HOURS -> false
            else -> throw IllegalArgumentException("Measurement: ${this.code} does not have assigned securities measurement")
        }

    fun securitiesForIndex() =
        when (this) {
            INDEX_DAILY_CHANGE_REGULAR_HOURS -> SECURITIES_DAILY_CHANGE_REGULAR_HOURS
            INDEX_DAILY_CHANGE_EXTENDED_HOURS -> SECURITIES_DAILY_CHANGE_EXTENDED_HOURS
            else -> throw IllegalArgumentException("Measurement: ${this.code} does not have assigned securities measurement")
        }

    companion object {
        private val map = entries.associateBy(Measurement::code)

        fun fromCode(code: String): Measurement =
            map[code] ?: throw IllegalArgumentException("Invalid code: $code for enum Measurement")

        val indexMeasurements = entries.filter { it.code.startsWith("index-") }
    }
}
