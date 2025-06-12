package net.anzop.gather.config

import net.anzop.gather.helpers.jakarta.UniqueCompany
import net.anzop.gather.helpers.jakarta.UniqueSymbols
import net.anzop.gather.model.SourceDataParams
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@ConfigurationProperties
@Validated
@UniqueSymbols
@UniqueCompany
data class SourceDataConfig(
    val sourceDataParams: List<SourceDataParams>
) {
    fun resolve(ticker: String): SourceDataParams? =
        sourceDataParams
            .filter { it.marketData.ticker == ticker }
            .singleOrNull()
}
