package net.anzop.processors.RegressionTrend

import net.anzop.sinks.DataSerializer

class TrendSegmentSerDes extends DataSerializer[TrendSegment] with Serializable {

  override def serialize(data: TrendSegment): String = {
    val timestamp = data.ends * 1000000L
    s"trend-segment begins=${data.begins},ends=${data.ends}," +
      s"trendAngleAnnualized=${setScale(data.trendAngleAnnualized)}," +
      s"regression_slope=${setScale(data.regressionSlope)}," +
      s"regression_intercept=${setScale(data.regressionIntercept)}," +
      s"regression_variance=${setScale(data.regressionVariance)} $timestamp"
  }
}

object TrendSegmentSerDes {
  def apply(): TrendSegmentSerDes = new TrendSegmentSerDes()
}

class ListTrendSegmentSerDes(segmentSerializer: DataSerializer[TrendSegment])
    extends DataSerializer[List[TrendSegment]]
    with Serializable {

  override def serialize(data: List[TrendSegment]): String =
    data.map(segmentSerializer.serialize).mkString("\n")
}

object ListTrendSegmentSerDes {

  def apply(segmentSerializer: DataSerializer[TrendSegment] = TrendSegmentSerDes()): ListTrendSegmentSerDes =
    new ListTrendSegmentSerDes(segmentSerializer)
}
