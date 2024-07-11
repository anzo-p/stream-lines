package net.anzop.retro.helpers

import java.net.URI
import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.web.reactive.function.client.ClientResponse
import org.springframework.web.reactive.function.client.WebClient
import reactor.core.publisher.Mono

inline fun <reified T> WebClient.getRequest(url: URI): T? =
    this
        .get()
        .uri(url)
        .retrieve()
        .onStatus(HttpStatusCode::isError) { it.handleError() }
        .bodyToMono(T::class.java)
        .block()

fun ClientResponse.handleError(): Mono<out Throwable> {
    val logger = org.slf4j.LoggerFactory.getLogger("net.anzop.retro.helpers.webclient.handleError")!!

    when (this.statusCode().value()) {
        HttpStatus.BAD_REQUEST.value() -> {
            logger.error("Bad request ${this.bodyToMono(String::class.java).block()}")
        }
        HttpStatus.UNAUTHORIZED.value() -> {
            logger.error("Unauthorized ${this.bodyToMono(String::class.java).block()}")
        }
        HttpStatus.FORBIDDEN.value() -> {
            logger.error("Forbidden ${this.bodyToMono(String::class.java).block()}")
        }
        HttpStatus.NOT_FOUND.value() -> {
            logger.error("Not found ${this.bodyToMono(String::class.java).block()}")
        }
        HttpStatus.INTERNAL_SERVER_ERROR.value() -> {
            logger.error("Internal server error ${this.bodyToMono(String::class.java).block()}")
        }
        else -> {
            logger.error("Unknown error ${this.bodyToMono(String::class.java).block()}")
        }
    }

    return this.createException()
}
