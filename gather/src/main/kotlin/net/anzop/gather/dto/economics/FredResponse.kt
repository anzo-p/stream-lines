package net.anzop.gather.dto.economics

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.time.LocalDate
import kotlinx.serialization.Serializable

interface FredObservation {
    val date: LocalDate
    val value: String
}

@JsonIgnoreProperties(ignoreUnknown = true)
@Serializable
data class FredResponse<T>(
    val observations: List<T>
)
