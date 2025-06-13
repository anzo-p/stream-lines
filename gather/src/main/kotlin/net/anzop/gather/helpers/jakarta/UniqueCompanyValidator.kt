package net.anzop.gather.helpers.jakarta

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import net.anzop.gather.config.SourceDataConfig

class UniqueCompanyValidator : ConstraintValidator<UniqueCompany, SourceDataConfig> {
    override fun isValid(config: SourceDataConfig, context: ConstraintValidatorContext): Boolean {
        val companySeries = mutableMapOf<String, MutableSet<String?>>()
        val duplicates = mutableSetOf<String>()

        config.params.forEach { settings ->
            val (_, companyName, stockSeries) = settings.marketData
            val series = companySeries.getOrPut(companyName) { mutableSetOf() }
            if (series.isNotEmpty() && stockSeries == null) {
                duplicates.add("$companyName (Series: null)")
            } else if (!series.add(stockSeries)) {
                duplicates.add("$companyName (Series: ${series})")
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
