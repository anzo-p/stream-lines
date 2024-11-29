package net.anzop.gather.config

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import com.influxdb.client.WriteApi
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated

@Configuration
@ConfigurationProperties(prefix = "influxdb")
@Validated
class InfluxDBConfig {

    @NotBlank
    lateinit var url: String

    @NotBlank
    lateinit var token: String

    @NotBlank
    lateinit var organization: String

    @NotBlank
    lateinit var bucket: String

    @Bean
    fun influxDBClient(): InfluxDBClient =
        InfluxDBClientFactory.create(url, token.toCharArray(), organization, bucket)

    @Bean
    fun influxDBAsyncWriter(): WriteApi =
        InfluxDBClientFactory.create(url, token.toCharArray(), organization, bucket).writeApi
}
