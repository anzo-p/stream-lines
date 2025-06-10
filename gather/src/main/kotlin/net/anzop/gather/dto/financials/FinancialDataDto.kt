package net.anzop.gather.dto.financials

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.anzop.gather.helpers.jakarta.AtLeastOneNonNull
import org.springframework.validation.annotation.Validated

@Serializable
@AtLeastOneNonNull
@Validated
data class FinancialDataDto(

    @SerialName("annual")
    val annualData: FinancialStatementsDto? = null,

    @SerialName("quarterly")
    val quarterlyData: FinancialStatementsDto? = null,
)


