package net.anzop.retro.service

import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import java.time.OffsetDateTime
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import net.anzop.retro.model.BarData
import net.anzop.retro.serdes.OffsetDateTimeSerializer

@Serializable
data class BarDataDto(
    @SerialName("o")
    @field:Min(0)
    val openingPrice: Double,

    @SerialName("c")
    @field:Min(0)
    val closingPrice: Double,

    @SerialName("h")
    @field:Min(0)
    val highPrice: Double,

    @SerialName("l")
    @field:Min(0)
    val lowPrice: Double,

    @SerialName("v")
    @field:Min(0)
    val volume: Long,

    @SerialName("vw")
    @field:Min(0)
    val volumeWeightedAvgPrice: Double,

    @SerialName("t")
    @Serializable(with = OffsetDateTimeSerializer::class)
    @field:NotNull
    val marketTimestamp: OffsetDateTime,
) {
    fun toModel(measurement: String, ticker: String): BarData =
        BarData(
            measurement = measurement,
            ticker = ticker,
            openingPrice = openingPrice,
            closingPrice = closingPrice,
            highPrice = highPrice,
            lowPrice = lowPrice,
            volumeWeightedAvgPrice = volumeWeightedAvgPrice,
            totalTradingValue = volumeWeightedAvgPrice * volume,
            marketTimestamp = marketTimestamp
        )
}

@Serializable
data class BarsResponse(
    val bars: Map<String, List<BarDataDto>>,

    @SerialName("next_page_token")
    val nextPageToken: String?
)
