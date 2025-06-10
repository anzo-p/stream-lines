package net.anzop.gather.dto.bars

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.OffsetDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.anzop.gather.dto.serdes.OffsetDateTimeSerializer
import net.anzop.gather.helpers.date.nyseTradingHours
import net.anzop.gather.helpers.jakarta.validate
import net.anzop.gather.model.SourceDataParams
import net.anzop.gather.model.marketData.BarData
import net.anzop.gather.model.marketData.Measurement
import org.springframework.validation.annotation.Validated

@Serializable
@Validated
data class BarDataDto(

    @SerialName("c")
    @field:Positive(message = "must be positive")
    val closingPrice: Double,

    @SerialName("h")
    @field:Positive(message = "must be positive")
    val highPrice: Double,

    @SerialName("l")
    @field:Positive(message = "must be positive")
    val lowPrice: Double,

    @SerialName("o")
    @field:Positive(message = "must be positive")
    val openingPrice: Double,

    @SerialName("t")
    @Serializable(with = OffsetDateTimeSerializer::class)
    @field:NotNull(message = "cannot be null")
    val marketTimestamp: OffsetDateTime,

    @SerialName("v")
    @field:Positive(message = "must be positive")
    val volume: Long,

    @SerialName("vw")
    @field:Positive(message = "must be positive")
    val volumeWeightedAvgPrice: Double,

) {
    fun toModel(measurement: Measurement, params: SourceDataParams): BarData {
        validate(this)
            .takeIf { it.isNotEmpty() }
            ?.let { violations ->
                val errorMessages = violations.joinToString("; ") { violation ->
                    val propertyName = violation.propertyPath.iterator().asSequence().last().name
                    "'$propertyName' ${violation.message}"
                }
                throw IllegalArgumentException("Validation failed for ${this::class.simpleName}: $errorMessages")
            }

        val utcTime = marketTimestamp.toInstant()

        return BarData(
            measurement = measurement,
            ticker = params.marketData.ticker,
            company = params.marketData.companyName,
            marketTimestamp = utcTime,
            regularTradingHours = nyseTradingHours.isOpenAt(utcTime),
            openingPrice = openingPrice,
            closingPrice = closingPrice,
            highPrice = highPrice,
            lowPrice = lowPrice,
            volumeWeightedAvgPrice = volumeWeightedAvgPrice,
            totalTradingValue = volumeWeightedAvgPrice * volume,
        )
    }
}

@Serializable
@Validated
data class BarsResponse(
    val bars: Map<String, List<BarDataDto>>,

    @SerialName("next_page_token")
    val nextPageToken: String?
)
