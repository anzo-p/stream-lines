package net.anzop.processors.Drawdown

import net.anzop.sinks.DataSerializer

class DrawdownSerDes(val measurementName: String) extends DataSerializer[Drawdown] with Serializable {

  override def serialize(data: Drawdown): String =
    s"$measurementName " +
      s"value=${setScale(data.value)}," +
      s"drawdown=${setScale(data.drawdown)} ${data.timestamp * 1000000L}"
}

object DrawdownSerDes {

  def apply(measurementName: String): DrawdownSerDes =
    new DrawdownSerDes(measurementName)
}
