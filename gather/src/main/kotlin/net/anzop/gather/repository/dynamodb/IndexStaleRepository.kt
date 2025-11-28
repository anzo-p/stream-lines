package net.anzop.gather.repository.dynamodb

import java.time.LocalDate
import net.anzop.gather.config.AwsConfig
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient

@Repository
class IndexStaleRepository(
    awsConfig: AwsConfig,
    dynamoDbClient: DynamoDbClient,
) : DynamoDbCore(awsConfig, dynamoDbClient) {

    fun suggestIndexStaleFrom(date: LocalDate) {
        getIndexStaleFrom()?.let {
            if (it.isBefore(date)) {
                return
            }
        }
        saveItem(
            mapOf(
                "pk" to "indexStaleFrom".toAttrib(),
                "sk" to "indexStaleFrom".toAttrib(),
                "indexStaleFrom" to date.toAttrib()
            )
        )
    }

    fun getIndexStaleFrom(): LocalDate? {
        val key = mapOf(
            "pk" to "indexStaleFrom".toAttrib(),
            "sk" to "indexStaleFrom".toAttrib()
        )
        return getItem(key)
            ?.get("indexStaleFrom")
            ?.s()
            ?.let { LocalDate.parse(it) }
    }

    fun deleteIndexStaleFrom() =
        deleteItem(
            mapOf(
                "pk" to "indexStaleFrom".toAttrib(),
                "sk" to "indexStaleFrom".toAttrib()
            )
        )
}
