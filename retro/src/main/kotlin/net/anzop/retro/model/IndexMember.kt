package net.anzop.retro.model

import java.time.LocalDate
import net.anzop.retro.model.marketData.Measurement

typealias IndexMembers = MutableMap<String, IndexMember>

data class PrevDayData(
    val date: LocalDate,
    val avgPrice: Double,
)

data class IndexMember (
    val ticker: String,
    val measurement: Measurement,
    val indexValueWhenIntroduced: Double,
    val introductionPrice: Double,
    val prevDayData: PrevDayData
)
