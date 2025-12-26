package net.anzop.processors

import net.anzop.Ripples.logger
import net.anzop.results.{BaseDelta, BaseWindow}
import org.apache.flink.api.common.state.{StateTtlConfig, ValueState, ValueStateDescriptor}
import org.apache.flink.api.common.time.Time
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

trait DeltaProcessor[IN <: BaseWindow, OUT <: BaseDelta] extends KeyedProcessFunction[String, IN, OUT] {

  implicit val typeInfoT: TypeInformation[IN]

  protected def compose(prev: IN, curr: IN): OUT

  @transient private var prevState: ValueState[IN] = _

  override def open(parameters: Configuration): Unit = {
    val ttl = StateTtlConfig
      .newBuilder(Time.hours(24))
      .setUpdateType(StateTtlConfig.UpdateType.OnCreateAndWrite)
      .setStateVisibility(StateTtlConfig.StateVisibility.NeverReturnExpired)
      .build()

    val desc = new ValueStateDescriptor[IN]("previousWindowState", typeInfoT)
    desc.enableTimeToLive(ttl)
    prevState = getRuntimeContext.getState(desc)
  }

  override def processElement(curr: IN, ctx: KeyedProcessFunction[String, IN, OUT]#Context, out: Collector[OUT]): Unit = {
    val prev = prevState.value()
    if (prev == null) {
      prevState.update(curr)
      logger.info(s"Initialized previous state for key ${ctx.getCurrentKey} at ts ${curr.timestamp}")
      return
    }
    if (prev != null && !curr.timestamp.isAfter(prev.timestamp)) {
      logger.warn(
        s"Dropping out-of-order or duplicate window for key ${ctx.getCurrentKey}: " +
          s"current window ts ${curr.timestamp}, previous window ts ${prev.timestamp}"
      )
      return
    }

    val result = compose(prev, curr)
    out.collect(result)

    prevState.update(curr)
  }
}
