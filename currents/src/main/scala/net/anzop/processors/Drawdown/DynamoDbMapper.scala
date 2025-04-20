package net.anzop.processors.Drawdown

import net.anzop.repository.dynamodb.AttribValMapper
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

protected object DynamoDbMapper {
  implicit val stateMapper: AttribValMapper[(Long, Double)] = new AttribValMapper[(Long, Double)] {
    override def pk: String = "DRAWDOWNSTATE"

    override def fromItem(item: Map[String, AttributeValue]): (Long, Double) = (
      item("timestamp").n.toLong,
      item("value").n.toDouble
    )

    override def toItem(value: (Long, Double)): Map[String, AttributeValue] =
      value match {
        case (ts, v) =>
          baseAttribs() + (
            "timestamp" -> AttributeValue.builder().n(ts.toString).build(),
            "value"     -> AttributeValue.builder().n(v.toString).build()
          )
      }

    override def toGetKey(value: (Long, Double)): Map[String, AttributeValue] = ???
  }
}
