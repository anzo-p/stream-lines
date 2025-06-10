package net.anzop.gather.dto.financials

import jakarta.validation.constraints.NotNull
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.springframework.validation.annotation.Validated

@Serializable
@Validated
data class CompanyInfoDto(

    @SerialName("cik")
    @field:NotNull(message = "cannot be null")
    val centralIndexKey: String,

    @field:NotNull(message = "cannot be null")
    val ticker: String,

    @SerialName("name")
    @field:NotNull(message = "cannot be null")
    val companyName: String,
)
