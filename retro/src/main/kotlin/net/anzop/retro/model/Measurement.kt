package net.anzop.retro.model

enum class Measurement(val code: String) {
    SECURITIES_RAW_DAILY("sec_raw_d"),
    SECURITIES_WEIGHTED_EQUAL_DAILY("sec_w_eq_d"),
    INDEX_WEIGHTED_EQUAL_DAILY("ix_w_eq_d");

    companion object {
        private val map = entries.associateBy(Measurement::code)

        fun fromCode(code: String): Measurement {
            return map[code] ?: throw IllegalArgumentException("Invalid code: $code for enum Measurement")
        }
    }
}
