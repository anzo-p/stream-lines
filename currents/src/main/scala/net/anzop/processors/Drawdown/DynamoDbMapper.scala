package net.anzop.processors.Drawdown

import net.anzop.repository.dynamodb.AttribValMapper
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

protected object DynamoDbMapper {
  implicit val stateMapper: AttribValMapper[(Long, (Double, Double, Double))] =
    new AttribValMapper[(Long, (Double, Double, Double))] {
      override def pk: String = "DRAWDOWNSTATE"

      override def fromItem(item: Map[String, AttributeValue]): (Long, (Double, Double, Double)) = (
        item("timestamp").n.toLong,
        (item("drawdownLow").n.toDouble, item("drawdownAvg").n.toDouble, item("drawdownHigh").n.toDouble)
      )

      override def toItem(value: (Long, (Double, Double, Double))): Map[String, AttributeValue] =
        value match {
          case (ts, (low, avg, high)) =>
            baseAttribs() + (
              "timestamp"    -> AttributeValue.builder().n(ts.toString).build(),
              "drawdownLow"  -> AttributeValue.builder().n(low.toString).build(),
              "drawdownAvg"  -> AttributeValue.builder().n(avg.toString).build(),
              "drawdownHigh" -> AttributeValue.builder().n(high.toString).build()
            )
        }

      override def toGetKey(value: (Long, (Double, Double, Double))): Map[String, AttributeValue] = ???
    }
}
