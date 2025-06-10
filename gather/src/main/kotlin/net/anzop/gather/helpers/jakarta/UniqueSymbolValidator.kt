package net.anzop.gather.helpers.jakarta

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import net.anzop.gather.config.SourceDataConfig

class UniqueSymbolValidator : ConstraintValidator<UniqueSymbols, SourceDataConfig> {
    override fun isValid(value: SourceDataConfig, context: ConstraintValidatorContext): Boolean {
        val symbols = mutableSetOf<String>()
        val duplicates = mutableSetOf<String>()

        value.sourceDataSettings.forEach {
            if (!symbols.add(it.marketData.ticker)) {
                duplicates.add(it.marketData.ticker)
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
