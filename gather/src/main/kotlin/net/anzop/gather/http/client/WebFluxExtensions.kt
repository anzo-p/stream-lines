package net.anzop.gather.http.client

import java.io.IOException
import java.net.URI
import java.time.Duration
import java.util.concurrent.TimeoutException
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import org.springframework.web.reactive.function.client.WebClientRequestException
import reactor.core.publisher.Mono
import reactor.util.retry.Retry

object WebFluxExtensions {
    val logger: Logger = LoggerFactory.getLogger(WebFluxExtensions::class.java)

    inline fun <reified T> WebClient.getRequest(url: URI): T? =
        this
            .get()
            .uri(url)
            .retrieve()
            .onStatus(HttpStatusCode::isError) { it.handleError() }
            .bodyToMono(object : ParameterizedTypeReference<T>() {})
            .timeout(Duration.ofSeconds(15))
            .retryWhen(
                Retry
                    .backoff(5, Duration.ofSeconds(3))
                    .maxBackoff(Duration.ofSeconds(30))
                    .filter { ex ->
                        ex is WebClientRequestException ||
                        ex is IOException || ex.cause is IOException ||
                        ex is TimeoutException || ex.cause is TimeoutException
                    }
                    .doBeforeRetry { retrySignal ->
                        logger.warn("Request to $url failed with ${retrySignal.failure().message}. " +
                                "Retrying... (attempt ${retrySignal.totalRetries() + 1})")
                    }
            )
            .block()

    fun ClientResponse.handleError(): Mono<Throwable> {
        val logger = LoggerFactory.getLogger(ClientResponse::class.java)

        return this.bodyToMono(String::class.java)
            .defaultIfEmpty("No response body")
            .flatMap { body ->
                when (this.statusCode().value()) {
                    HttpStatus.BAD_REQUEST.value() -> {
                        logger.error("Bad request: $body")
                    }
                    HttpStatus.UNAUTHORIZED.value() -> {
                        logger.error("Unauthorized: $body")
                    }
                    HttpStatus.FORBIDDEN.value() -> {
                        logger.error("Forbidden: $body")
                    }
                    HttpStatus.NOT_FOUND.value() -> {
                        logger.error("Not found: $body")
                    }
                    HttpStatus.TOO_MANY_REQUESTS.value() -> {
                        logger.error("Too many requests: $body")
                    }
                    HttpStatus.INTERNAL_SERVER_ERROR.value() -> {
                        logger.error("Internal server error: $body")
                    }
                    else -> {
                        logger.error("Unknown error: $body")
                    }
                }
                Mono.error(RuntimeException("HTTP Error ${this.statusCode().value()}: $body"))
            }
    }
}
