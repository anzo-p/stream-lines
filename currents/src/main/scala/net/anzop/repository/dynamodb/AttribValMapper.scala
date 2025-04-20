package net.anzop.repository.dynamodb

import software.amazon.awssdk.services.dynamodb.model._

trait AttribValMapper[T] {

  def baseAttribs(expression: Boolean = false): Map[String, AttributeValue] =
    Map(
      "pk" -> pk,
      "sk" -> sk
    ).map {
      case (k, v) =>
        (if (expression) s":$k" else k) -> AttributeValue.builder().s(v).build()
    }

  def sk: String = "VALUE"

  def pk: String
  def fromItem(item: Map[String, AttributeValue]): T
  def toItem(value: T): Map[String, AttributeValue]
  def toGetKey(value: T): Map[String, AttributeValue]
}
