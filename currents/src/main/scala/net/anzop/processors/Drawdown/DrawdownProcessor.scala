package net.anzop.processors.Drawdown

import net.anzop.models.MarketData
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import java.time.Instant
import java.time.temporal.ChronoUnit

class DrawdownProcessor extends KeyedProcessFunction[String, MarketData, Drawdown] {
  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  private val startingState: (Long, Double)             = (0L, 0.0)
  private var maxValueState: ValueState[(Long, Double)] = _

  override def open(parameters: Configuration): Unit = {
    val descriptor = new ValueStateDescriptor[(Long, Double)]("maxValueState", classOf[(Long, Double)])
    maxValueState = getRuntimeContext.getState(descriptor)
  }

  override def processElement(
      elem: MarketData,
      ctx: KeyedProcessFunction[String, MarketData, Drawdown]#Context,
      out: Collector[Drawdown]
    ): Unit = {
    val (lastTs, lastMaxValue) = Option(maxValueState.value()).getOrElse(startingState)

    if (elem.timestamp <= lastTs) {
      logger.info(s"Skipping out-of-order event: $elem (latest timestamp: $lastTs)")
      return
    }

    val updatedMaxValue = Math.max(lastMaxValue, elem.value)

    val updatedTs = {
      if (Instant.ofEpochMilli(elem.timestamp).isBefore(Instant.now().truncatedTo(ChronoUnit.DAYS)))
        elem.timestamp
      else
        lastTs
    }

    maxValueState.update(updatedTs -> updatedMaxValue)

    out.collect(
      Drawdown(
        timestamp = elem.timestamp,
        field     = elem.field,
        value     = elem.value,
        drawdown  = (elem.value / updatedMaxValue) * 100
      ))
  }
}
