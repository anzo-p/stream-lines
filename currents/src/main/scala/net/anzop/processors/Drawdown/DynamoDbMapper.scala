package net.anzop.processors.Drawdown

import net.anzop.repository.dynamodb.AttribValMapper
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

protected object DynamoDbMapper {

  implicit val stateMapper: AttribValMapper[DrawdownState] =
    new AttribValMapper[DrawdownState] {
      override def pk: String = "DRAWDOWNSTATE"

      override def fromItem(item: Map[String, AttributeValue]): DrawdownState = DrawdownState(
        item("timestamp").n.toLong,
        item("drawdownLow").n.toDouble,
        item("drawdownAvg").n.toDouble,
        item("drawdownHigh").n.toDouble
      )

      override def toItem(value: DrawdownState): Map[String, AttributeValue] =
        value match {
          case DrawdownState(ts, low, avg, high) =>
            baseAttribs() + (
              "timestamp"    -> AttributeValue.builder().n(ts.toString).build(),
              "drawdownLow"  -> AttributeValue.builder().n(low.toString).build(),
              "drawdownAvg"  -> AttributeValue.builder().n(avg.toString).build(),
              "drawdownHigh" -> AttributeValue.builder().n(high.toString).build()
            )
        }

      override def toGetKey(value: DrawdownState): Map[String, AttributeValue] = ???
    }
}
