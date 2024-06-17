package net.anzop.retro.config

import com.influxdb.client.InfluxDBClient
import com.influxdb.client.InfluxDBClientFactory
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
@ConfigurationProperties(prefix = "influxdb")
class InfluxDBConfig {

    lateinit var url: String
    lateinit var token: String
    lateinit var organization: String
    lateinit var bucket: String

    @Bean
    fun influxDBClient(): InfluxDBClient = InfluxDBClientFactory.create(url, token.toCharArray(), organization, bucket)
}
