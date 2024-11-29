package net.anzop.gather.repository.dynamodb

import java.time.LocalDate
import net.anzop.gather.config.AwsConfig
import net.anzop.gather.model.IndexMembers
import net.anzop.gather.model.marketData.Measurement
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest

@Repository
class CacheRepository(
    private val awsConfig: AwsConfig,
    private val dynamoDbClient: DynamoDbClient
) {
    fun suggestIndexStaleFrom(date: LocalDate) {
        getIndexStaleFrom()?.let {
            if (it.isBefore(date)) {
                return
            }
        }
        saveItem(
            mapOf(
                "PK" to "indexStaleFrom".toAttrib(),
                "SK" to "indexStaleFrom".toAttrib(),
                "indexStaleFrom" to date.toAttrib()
            )
        )
    }

    fun getIndexStaleFrom(): LocalDate? {
        val key = mapOf(
            "PK" to "indexStaleFrom".toAttrib(),
            "SK" to "indexStaleFrom".toAttrib()
        )
        return getItem(key)
            ?.get("indexStaleFrom")
            ?.s()
            ?.let { LocalDate.parse(it) }
    }

    fun deleteIndexStaleFrom() =
        deleteItem(
            mapOf(
                "PK" to "indexStaleFrom".toAttrib(),
                "SK" to "indexStaleFrom".toAttrib()
            )
        )

    fun storeMemberSecurities(measurement: Measurement, securities: IndexMembers) =
        saveItem(
            mapOf(
                "PK" to "memberSecurity".toAttrib(),
                "SK" to "measurement#${measurement.code}".toAttrib(),
                "memberSecurities" to securities.toAttrib()
            )
        )

    fun getMemberSecurities(measurement: Measurement): IndexMembers {
        val key = mapOf(
            "PK" to "memberSecurity".toAttrib(),
            "SK" to "measurement#${measurement.code}".toAttrib(),
        )
        return getItem(key)
            ?.let { getMemberSecurities(it) }
            ?: mutableMapOf()
    }

    fun deleteMemberSecurities(measurement: Measurement) =
        deleteItem(
            mapOf(
                "PK" to "memberSecurity".toAttrib(),
                "SK" to "measurement#${measurement.code}".toAttrib(),
            )
        )

    private fun saveItem(item: AttribMap) {
        val request = PutItemRequest.builder()
            .tableName(awsConfig.dynamodb.tableName)
            .item(item)
            .build()

        dynamoDbClient.putItem(request)
    }

    private fun getItem(key: AttribMap): AttribMap? {
        val request = GetItemRequest.builder()
            .tableName(awsConfig.dynamodb.tableName)
            .key(key)
            .build()

        return dynamoDbClient.getItem(request).item()
    }

    private fun deleteItem(key: AttribMap) {
        val request = DeleteItemRequest.builder()
            .tableName(awsConfig.dynamodb.tableName)
            .key(key)
            .build()

        dynamoDbClient.deleteItem(request)
    }
}
