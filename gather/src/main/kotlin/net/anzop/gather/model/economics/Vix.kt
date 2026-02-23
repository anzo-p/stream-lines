package net.anzop.gather.model.economics

import java.time.LocalDate
import net.anzop.gather.model.marketData.Measurement

data class Vix(
    val date: LocalDate,
    val value: Double
) {
    val measurement = MEASUREMENT
    val ticker = TICKER

    companion object {
        val MEASUREMENT: Measurement = Measurement.VIX_DAILY
        const val TICKER: String = "^VIX"
        const val FRED_TICKER: String = "VIXCLS"
    }
}
