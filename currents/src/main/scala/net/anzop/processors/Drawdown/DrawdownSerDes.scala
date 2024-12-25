package net.anzop.processors.Drawdown

import net.anzop.sinks.DataSerializer

class DrawdownSerDes extends DataSerializer[Drawdown] with Serializable {

  override def serialize(data: Drawdown): String = {
    val timestamp = data.timestamp * 1000000L
    s"drawdown " +
      s"value=${setScale(data.value)}," +
      s"drawdown=${setScale(data.drawdown)} $timestamp"
  }
}

object DrawdownSerDes {
  def apply(): DrawdownSerDes = new DrawdownSerDes()
}
