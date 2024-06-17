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
    @SerialName("o") @field:Min(0) val openingPrice: Double,
    @SerialName("c") @field:Min(0) val closingPrice: Double,
    @SerialName("h") @field:Min(0) val highPrice: Double,
    @SerialName("l") @field:Min(0) val lowPrice: Double,
    @SerialName("n") @field:Min(0) val numBerOfTrades: Long,
    @SerialName("v") @field:Min(0) val volume: Long,
    @SerialName("vw") @field:Min(0) val volumeWeighted: Double,
    @SerialName("t") @field:NotNull @Serializable(with = OffsetDateTimeSerializer::class) val marketTimestamp: OffsetDateTime,
) {
    fun toModel(ticker: String): BarData {
        return BarData(
            ticker = ticker,
            barTimeSpan = "1Day",
            openingPrice = openingPrice,
            closingPrice = closingPrice,
            highPrice = highPrice,
            lowPrice = lowPrice,
            numberOfTrades = numBerOfTrades,
            volume = volume,
            volumeWeighted = volumeWeighted,
            marketTimestamp = marketTimestamp,
        )
    }
}
