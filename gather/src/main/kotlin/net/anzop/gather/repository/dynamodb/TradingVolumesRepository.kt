package net.anzop.gather.repository.dynamodb

import java.math.BigDecimal
import net.anzop.gather.config.AwsConfig
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

@Repository
class TradingVolumesRepository(
    awsConfig: AwsConfig,
    dynamoDbClient: DynamoDbClient,
) : DynamoDbCore(awsConfig, dynamoDbClient) {

    fun storeTradingVolumes(tradingVolumes: Map<String, Map<String, BigDecimal>>) =
        tradingVolumes.forEach { (key, volumes) ->
            saveItem(
                mapOf(
                    "pk" to "monthlyTradingVolumes".toAttrib(),
                    "sk" to "year-month#${key}".toAttrib(),
                    "tradingVolumes" to volumes.toAttrib()
                )
            )
        }

    fun getTradingVolumes(key: String): Map<String, BigDecimal> {
        val key = mapOf(
            "pk" to "monthlyTradingVolumes".toAttrib(),
            "sk" to "year-month#${key}".toAttrib(),
        )
        return getItem(key)
            ?.let { it["tradingVolumes"]?.toMapBigDecimalValues() }
            ?: mutableMapOf()
    }

    private fun Map<String, BigDecimal>.toAttrib(): AttributeValue =
        AttributeValue.builder().m(
            this.mapValues { AttributeValue.builder().n(it.value.toString()).build() }
        ).build()

    private fun AttributeValue?.toMapBigDecimalValues(): Map<String, BigDecimal> =
        this?.m()?.mapValues { (_, v) -> BigDecimal(v.n()) } ?: emptyMap()
}
