package net.anzop.processors.Trend

import net.anzop.sinks.DataSerializer

class TrendSegmentSerDes(val measurementName: String) extends DataSerializer[List[TrendSegment]] with Serializable {

  private def serializeOne(data: TrendSegment): String =
    s"$measurementName begins=${data.begins},ends=${data.ends}," +
      s"growth=${setScale(data.growth)}," +
      s"regression_slope=${setScale(data.regressionSlope)}," +
      s"regression_intercept=${setScale(data.regressionIntercept)}," +
      s"regression_variance=${setScale(data.regressionVariance)} ${data.ends * 1000000L}"

  def serialize(data: TrendSegment): String =
    serialize(List(data))

  override def serialize(data: List[TrendSegment]): String =
    data.map(serializeOne).mkString("\n")
}

object TrendSegmentSerDes {

  def apply(measurementName: String): TrendSegmentSerDes =
    new TrendSegmentSerDes(measurementName)
}
