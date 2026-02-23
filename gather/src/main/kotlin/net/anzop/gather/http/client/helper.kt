package net.anzop.gather.http.client

import java.net.URI
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import net.anzop.gather.model.economics.Vix
import net.anzop.gather.model.financials.ReportPeriodType
import org.springframework.web.util.UriComponentsBuilder

fun buildGetFinancialsUri(
    baseUrl: URI,
    apiKey: String,
    symbol: String,
    period: ReportPeriodType
): URI {
    val builder = UriComponentsBuilder
        .fromHttpUrl(baseUrl.toString())
        .queryParam("apikey", apiKey)
        .queryParam("ticker", symbol)
        .queryParam("period", period.code)

    return URI.create(builder.toUriString())
}

fun buildGetHistoricalBarsUri(
    baseUrl: URI,
    feed: String,
    symbols: List<String>,
    timeframe: String,
    start: OffsetDateTime,
    end: OffsetDateTime? = null,
    adjustment: String? = "all",
    limit: Int? = null,
    pageToken: String? = null
): URI {
    val builder = UriComponentsBuilder
        .fromHttpUrl(baseUrl.toString())
        .queryParam("feed", feed)
        .queryParam("symbols", symbols.joinToString(","))
        .queryParam("timeframe", timeframe)
        .queryParam("start", start.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME))

    end?.let { builder.queryParam("end", end.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)) }
    adjustment?.let { builder.queryParam("adjustment", adjustment) }
    limit?.let {builder.queryParam("limit", limit) }
    pageToken?.let { builder.queryParam("page_token", pageToken) }

    return URI.create(builder.toUriString())
}

fun buildGetVixUri(
    baseUrl: URI,
    apiKey: String,
    startDate: LocalDate,
    endDate: LocalDate? = null
): URI {
    val builder = UriComponentsBuilder
        .fromHttpUrl(baseUrl.toString())
        .queryParam("api_key", apiKey)
        .queryParam("series_id", Vix.FRED_TICKER)
        .queryParam("file_type", "json")
        .queryParam("observation_start", startDate.format(DateTimeFormatter.ISO_LOCAL_DATE))

    endDate?.let { builder.queryParam("observation_end", it.format(DateTimeFormatter.ISO_LOCAL_DATE)) }

    return URI.create(builder.toUriString())
}
