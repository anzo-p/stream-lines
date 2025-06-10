package net.anzop.gather.http.client

import kotlinx.serialization.json.Json
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClient

@Component
class WebClientFactory {

    private val json = Json { ignoreUnknownKeys = true }

    fun builder(): WebClient.Builder =
        WebClient.builder()
            .codecs { config ->
                config.defaultCodecs().kotlinSerializationJsonDecoder(KotlinSerializationJsonDecoder(json))
                config.defaultCodecs().kotlinSerializationJsonEncoder(KotlinSerializationJsonEncoder(json))
            }
}