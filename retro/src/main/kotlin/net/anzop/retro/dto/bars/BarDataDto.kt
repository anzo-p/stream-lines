package net.anzop.retro.dto.bars

import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Positive
import java.time.OffsetDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.anzop.retro.dto.bars.serdes.OffsetDateTimeSerializer
import net.anzop.retro.helpers.date.nyseTradingHours
import net.anzop.retro.model.Ticker
import net.anzop.retro.model.marketData.BarData
import net.anzop.retro.model.marketData.Measurement
import org.springframework.validation.annotation.Validated

@Serializable
@Validated
data class BarDataDto(

    @SerialName("c")
    @field:Positive
    val closingPrice: Double,

    @SerialName("h")
    @field:Positive
    val highPrice: Double,

    @SerialName("l")
    @field:Positive
    val lowPrice: Double,

    @SerialName("o")
    @field:Positive
    val openingPrice: Double,

    @SerialName("t")
    @Serializable(with = OffsetDateTimeSerializer::class)
    @field:NotNull
    val marketTimestamp: OffsetDateTime,

    @SerialName("v")
    @field:Positive
    val volume: Long,

    @SerialName("vw")
    @field:Positive
    val volumeWeightedAvgPrice: Double,

) {
    fun toModel(measurement: Measurement, ticker: Ticker): BarData {
        validate(this).takeIf { it.isNotEmpty() }?.let {
            throw IllegalArgumentException("Validation failed: $this fails in $it")
        }

        val time = marketTimestamp.toInstant()

        return BarData(
            measurement = measurement,
            ticker = ticker.symbol,
            company = ticker.company,
            marketTimestamp = time,
            regularTradingHours = nyseTradingHours.isOpenAt(time),
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
