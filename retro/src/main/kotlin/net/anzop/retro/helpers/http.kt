package net.anzop.retro.helpers

import java.net.URI
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import org.springframework.web.util.UriComponentsBuilder

fun buildHistoricalBarsUri(
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
    val builder = UriComponentsBuilder.fromHttpUrl(baseUrl.toString())
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
