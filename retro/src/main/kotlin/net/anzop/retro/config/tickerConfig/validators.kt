package net.anzop.retro.config.tickerConfig

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext

class UniqueSymbolValidator : ConstraintValidator<UniqueSymbols, TickerConfig> {
    override fun isValid(value: TickerConfig, context: ConstraintValidatorContext): Boolean {
        val symbols = mutableSetOf<String>()
        val duplicates = mutableSetOf<String>()

        value.tickers.forEach {
            if (!symbols.add(it.symbol)) {
                duplicates.add(it.symbol)
            }
        }

        return if (duplicates.isNotEmpty()) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate("Duplicate symbols: $duplicates").addConstraintViolation()
            false
        } else {
            true
        }
    }
}

class UniqueCompanyValidator : ConstraintValidator<UniqueCompany, TickerConfig> {
    override fun isValid(value: TickerConfig, context: ConstraintValidatorContext): Boolean {
        val companySeries = mutableMapOf<String, MutableSet<String?>>()
        val duplicates = mutableSetOf<String>()

        value.tickers.forEach { ticker ->
            val series = companySeries.getOrPut(ticker.company) { mutableSetOf() }
            if (series.isNotEmpty() && ticker.series == null) {
                duplicates.add("${ticker.company} (Series: null)")
            } else if (!series.add(ticker.series)) {
                duplicates.add("${ticker.company} (Series: ${ticker.series})")
            }
        }

        return if (duplicates.isNotEmpty()) {
            context.disableDefaultConstraintViolation()
            context.buildConstraintViolationWithTemplate(
                "Invalid series makes duplicate company: ${duplicates.joinToString(", ")}"
            ).addConstraintViolation()
            false
        } else {
            true
        }
    }
}
