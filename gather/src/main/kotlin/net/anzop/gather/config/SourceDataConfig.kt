package net.anzop.gather.config

import jakarta.annotation.PostConstruct
import net.anzop.gather.helpers.jakarta.UniqueCompany
import net.anzop.gather.helpers.jakarta.UniqueSymbols
import net.anzop.gather.model.SourceDataParams
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.stereotype.Component
import org.springframework.validation.annotation.Validated

@Component
@ConfigurationProperties("source-data")
@Validated
@UniqueSymbols
@UniqueCompany
data class SourceDataConfig(
    var params: List<SourceDataParams> = emptyList()
) {
    @PostConstruct
    fun registerGlobally() {
        _instance = this
    }

    companion object {
        @Volatile
        private var _instance: SourceDataConfig? = null

        val instance: SourceDataConfig
            get() = _instance ?: error("SourceDataConfig not initialized")

        fun resolve(ticker: String): SourceDataParams? =
            instance.params.find { ticker in setOf(it.marketData.ticker, it.fundamentals?.ticker) }
    }
}
