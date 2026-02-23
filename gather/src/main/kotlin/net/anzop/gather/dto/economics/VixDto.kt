package net.anzop.gather.dto.economics

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import kotlinx.serialization.Serializable
import net.anzop.gather.dto.serdes.LocalDateSerializer
import net.anzop.gather.model.economics.Vix
import org.springframework.validation.annotation.Validated

typealias VixResponse = FredResponse<VixDto>

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
@Validated
data class VixDto(

    @Serializable(with = LocalDateSerializer::class)
    override val date: LocalDate,

    override val value: String
) : FredObservation
{
    fun toModel(): Vix? =
        value.trim()
            .takeUnless { it.isEmpty() || it == "." }
            ?.toDoubleOrNull()
            ?.let { Vix(date, it) }
}
