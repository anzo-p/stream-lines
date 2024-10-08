package net.anzop.retro.config

import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.validation.annotation.Validated
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider
import software.amazon.awssdk.regions.Region
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

@Configuration
@ConfigurationProperties(prefix = "aws")
@Validated
class AwsConfig {
    class DynamoDbProperties {
        @NotBlank
        lateinit var tableName: String
    }

    @NotBlank
    lateinit var region: String

    @Valid
    lateinit var dynamodb: DynamoDbProperties

    @Bean
    fun dynamoDbClient(): DynamoDbClient {
        return DynamoDbClient.builder()
            .region(Region.of(region))
            .credentialsProvider(DefaultCredentialsProvider.create())
            .build()
    }
}
