package net.anzop.retro.model

data class IndexMember (
    val indexValueWhenIntroduced: Double,
    val introductionPrice: Double,
    val prevDayPrice: Double,
    val ticker: String
)
