package net.anzop.gather.helpers.jakarta

import jakarta.validation.ConstraintValidator
import jakarta.validation.ConstraintValidatorContext
import net.anzop.gather.dto.financials.FinancialDataDto

class AtLeastOneNonNullValidator : ConstraintValidator<AtLeastOneNonNull, FinancialDataDto> {
    override fun isValid(dto: FinancialDataDto, context: ConstraintValidatorContext): Boolean =
        dto.annualData != null || dto.quarterlyData != null
}
