package net.anzop.gather.http.server

import java.math.BigDecimal
import java.math.RoundingMode
import java.time.YearMonth
import net.anzop.gather.config.SourceDataConfig
import net.anzop.gather.helpers.date.asAmericaNyToInstant
import net.anzop.gather.model.marketData.BarData
import net.anzop.gather.model.marketData.Measurement
import net.anzop.gather.repository.dynamodb.TradingVolumesRepository
import net.anzop.gather.repository.influxdb.MarketDataFacade
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/")
class QueryController(
    private val marketDataFacade: MarketDataFacade,
    private val sourceDataConfig: SourceDataConfig,
    private val tradingVolumesRepository: TradingVolumesRepository,
) {
    @GetMapping("/market-data/total-trading-volumes")
    fun fetchFinancials(): ResponseEntity<Map<String, Map<String, BigDecimal>>> {
        val ym = YearMonth.now().minusMonths(1)
        val key = "%04d-%02d".format(ym.year, ym.monthValue)
        val tradingVolumes = tradingVolumesRepository.getTradingVolumes(key)
            .takeIf { it.isNotEmpty() }
            ?: service(ym).also { fresh ->
                tradingVolumesRepository.storeTradingVolumes(mapOf(key to fresh))
            }

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(mapOf(key to tradingVolumes))
    }

    private fun service(ym: YearMonth): Map<String, BigDecimal> = sourceDataConfig
        .params
        .map { it.marketData.ticker }
        .associateWith { ticker ->
            marketDataFacade.getMeasurements(
                measurement = Measurement.SECURITIES_DAILY_BARS_RAW,
                from = ym.atDay(1).asAmericaNyToInstant(),
                til  = ym.atEndOfMonth().asAmericaNyToInstant(),
                ticker = ticker,
                clazz = BarData::class.java
            )
                .sumOf { it.totalTradingValue }
                .let { BigDecimal.valueOf(it) }
                .setScale(2, RoundingMode.HALF_UP)
        }
}
