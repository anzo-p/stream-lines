package net.anzop.gather.repository.dynamodb

import java.time.LocalDate
import net.anzop.gather.config.AwsConfig
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue
import software.amazon.awssdk.services.dynamodb.model.DeleteItemRequest
import software.amazon.awssdk.services.dynamodb.model.GetItemRequest
import software.amazon.awssdk.services.dynamodb.model.PutItemRequest
import software.amazon.awssdk.services.dynamodb.model.QueryRequest

typealias AttribMap = Map<String, AttributeValue>

open class DynamoDbCore(
    val awsConfig: AwsConfig,
    val dynamoDbClient: DynamoDbClient
) {
    protected fun Number.toAttrib(): AttributeValue =
        AttributeValue.builder().n(this.toString()).build()

    protected fun String.toAttrib(): AttributeValue =
        AttributeValue.builder().s(this).build()

    protected fun LocalDate.toAttrib(): AttributeValue =
        AttributeValue.builder().s(this.toString()).build()

    protected fun AttributeValue?.toIntOrNull(): Int? =
        this?.n()?.toIntOrNull()

    protected fun AttributeValue?.toLongOrNull(): Long? =
        this?.n()?.toLongOrNull()

    protected fun AttributeValue?.toDoubleOrNull(): Double? =
        this?.n()?.toDoubleOrNull()

    protected fun AttributeValue?.toStringOrNull(): String? =
        this?.s()

    protected fun AttributeValue?.toIntOrError(msg: String): Int =
        this?.toIntOrNull() ?: error(msg)

    protected fun AttributeValue?.toLongOrError(msg: String): Long =
        this?.toLongOrNull() ?: error(msg)

    protected fun AttributeValue?.toDoubleOrError(msg: String): Double =
        this?.toDoubleOrNull() ?: error(msg)

    protected fun AttributeValue?.toStringOrError(msg: String): String =
        this?.toStringOrNull() ?: error(msg)

    protected fun AttributeValue?.toIntOrDefault(default: Int = 0): Int =
        this.toIntOrNull() ?: default

    protected fun AttributeValue?.toLongOrDefault(default: Long = 0L): Long =
        this.toLongOrNull() ?: default

    protected fun AttributeValue?.toDoubleOrDefault(default: Double = 0.0): Double =
        this.toDoubleOrNull() ?: default

    protected fun AttributeValue?.toStringOrDefault(default: String = ""): String =
        this.toStringOrNull() ?: default

    protected fun <T> AttributeValue?.toListOrDefault(
        default: List<T> = emptyList(),
        mapper: (AttributeValue) -> T
    ): List<T> =
        this?.l()?.map { mapper(it) } ?: default

    protected fun AttributeValue?.toMapOrDefault(default: AttribMap = emptyMap()): AttribMap =
        this?.m() ?: default

    protected fun <T> convertAttributeValueMap(
        attributes: AttribMap,
        converter: (AttribMap) -> T?
    ): Map<String, T?> =
        attributes.mapValues { (_, value) ->
            converter(value.m())
        }

    protected fun saveItem(item: AttribMap) {
        val request = PutItemRequest.builder()
            .tableName(awsConfig.dynamodb.tableName)
            .item(item)
            .build()

        dynamoDbClient.putItem(request)
    }

    protected fun getItem(key: AttribMap): AttribMap? {
        val request = GetItemRequest.builder()
            .tableName(awsConfig.dynamodb.tableName)
            .key(key)
            .build()

        return dynamoDbClient.getItem(request).item()
    }

    private fun queryByPkBase(
        pkValue: String
    ): QueryRequest.Builder =
        QueryRequest.builder()
            .tableName(awsConfig.dynamodb.tableName)
            .keyConditionExpression("PK = :pk")
            .expressionAttributeValues(mapOf(":pk" to AttributeValue.builder().s(pkValue).build()))


    protected fun queryItems(
        pkValue: String
    ): List<AttribMap> =
        dynamoDbClient
            .query(queryByPkBase(pkValue).build())
            .items()

    protected fun querySortKeys(
        pkValue: String
    ): List<AttribMap> =
        dynamoDbClient
            .query(
                queryByPkBase(pkValue)
                    .projectionExpression("SK")
                    .build()
            )
            .items()

    protected fun deleteItem(key: AttribMap) {
        val request = DeleteItemRequest.builder()
            .tableName(awsConfig.dynamodb.tableName)
            .key(key)
            .build()

        dynamoDbClient.deleteItem(request)
    }
}
