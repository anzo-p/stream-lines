package net.anzop.retro.model

import net.anzop.retro.model.marketData.Measurement

data class IndexMember (
    val ticker: String,
    val measurement: Measurement,
    val indexValueWhenIntroduced: Double,
    val introductionPrice: Double,
    val prevDayPrice: Double,
)
