package net.anzop.gather.http.client

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import io.netty.handler.timeout.WriteTimeoutHandler
import java.time.Duration
import java.util.concurrent.TimeUnit
import kotlinx.serialization.json.Json
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.http.codec.json.KotlinSerializationJsonDecoder
import org.springframework.http.codec.json.KotlinSerializationJsonEncoder
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.ExchangeStrategies
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.http.client.HttpClient

@Component
class WebClientFactory {

    private val connectTimeout = Duration.ofSeconds(5)
    private val responseTimeout = Duration.ofSeconds(20)
    private val readTimeout = Duration.ofSeconds(20)
    private val writeTimeout = Duration.ofSeconds(20)

    private val json = Json { ignoreUnknownKeys = true }

    fun builder(): WebClient.Builder {
        val httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout.toMillis().toInt())
            .responseTimeout(responseTimeout)
            .doOnConnected { conn ->
                conn.addHandlerLast(ReadTimeoutHandler(readTimeout.toSeconds(), TimeUnit.SECONDS))
                conn.addHandlerLast(WriteTimeoutHandler(writeTimeout.toSeconds(), TimeUnit.SECONDS))
            }

        val strategies = ExchangeStrategies.builder()
            .codecs { config ->
                config.defaultCodecs().kotlinSerializationJsonDecoder(KotlinSerializationJsonDecoder(json))
                config.defaultCodecs().kotlinSerializationJsonEncoder(KotlinSerializationJsonEncoder(json))
            }
            .build()

        return WebClient.builder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .exchangeStrategies(strategies)
    }
}
