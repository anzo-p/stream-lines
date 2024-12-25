package net.anzop.processors.Drawdown

import net.anzop.models.MarketData
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

class DrawdownProcessor extends KeyedProcessFunction[String, MarketData, Drawdown] {
  private val startingValue: Double = 0.0

  private var maxValueState: ValueState[Double] = _

  override def open(parameters: Configuration): Unit = {
    val descriptor = new ValueStateDescriptor[Double]("maxValue", classOf[Double])
    maxValueState = getRuntimeContext.getState(descriptor)
  }

  override def processElement(
      value: MarketData,
      ctx: KeyedProcessFunction[String, MarketData, Drawdown]#Context,
      out: Collector[Drawdown]
    ): Unit = {
    val currentMax = Option(maxValueState.value()).getOrElse(startingValue)

    val updatedMax = Math.max(currentMax, value.value)
    maxValueState.update(updatedMax)

    val drawdown = (value.value / updatedMax) * 100

    out.collect(Drawdown(value.timestamp, value.field, value.value, drawdown))
  }
}
