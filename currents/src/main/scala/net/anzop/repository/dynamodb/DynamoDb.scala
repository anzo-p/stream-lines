package net.anzop.repository.dynamodb

import net.anzop.config.DynamoDbConfig
import software.amazon.awssdk.services.dynamodb.model._

import scala.jdk.CollectionConverters._

object DynamoDb {

  def save[T](value: T)(implicit mapper: AttribValMapper[T]): Unit = {
    val request = PutItemRequest
      .builder()
      .tableName(DynamoDbConfig.tableName)
      .item(mapper.toItem(value).asJava)
      .build()

    DynamoDbConfig.client.putItem(request)
  }

  def get[T](value: T)(implicit mapper: AttribValMapper[T]): Option[T] = {
    val request = GetItemRequest
      .builder()
      .tableName(DynamoDbConfig.tableName)
      .key(mapper.toGetKey(value).asJava)
      .build()

    DynamoDbConfig.client.getItem(request) match {
      case response if response.hasItem =>
        Some(mapper.fromItem(response.item.asScala.toMap))
      case _ =>
        None
    }
  }

  def getSingle[T](implicit mapper: AttribValMapper[T]): Option[T] = {
    val request = QueryRequest
      .builder()
      .tableName(DynamoDbConfig.tableName)
      .keyConditionExpression("pk = :pk and sk = :sk")
      .expressionAttributeValues(mapper.baseAttribs(expression = true).asJava)
      .build()

    val response = DynamoDbConfig.client.query(request)
    val items    = response.items.asScala.map(_.asScala.toMap).toList

    items match {
      case Nil         => None
      case item :: Nil => Some(mapper.fromItem(item))
      case _ =>
        throw new IllegalStateException(
          s"Expected at most one item for pk=${mapper.pk}, sk=${mapper.sk}, but found ${items.size}")
    }
  }
}
