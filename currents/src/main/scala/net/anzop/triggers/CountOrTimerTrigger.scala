package net.anzop.triggers

import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.streaming.api.windowing.triggers.{Trigger, TriggerResult}
import org.apache.flink.streaming.api.windowing.windows.GlobalWindow

class CountOrTimerTrigger(triggerCount: Long, triggerInterval: Long) extends Trigger[Any, GlobalWindow] {

  private def getPartitionedState(ctx: Trigger.TriggerContext, name: String): ValueState[Long] =
    ctx.getPartitionedState(new ValueStateDescriptor(name, classOf[Long]))

  override def onElement(
      element: Any,
      timestamp: Long,
      window: GlobalWindow,
      ctx: Trigger.TriggerContext
    ): TriggerResult = {
    val countState   = getPartitionedState(ctx, "count")
    val currentCount = Option(countState.value()).getOrElse(0L)
    val updatedCount = currentCount + 1
    countState.update(updatedCount)

    val lastFiredState = getPartitionedState(ctx, "lastFiredTime")
    val lastFiredTime  = Option(lastFiredState.value()).getOrElse(0L)

    val currentProcessingTime = ctx.getCurrentProcessingTime
    val nextTriggerTime       = lastFiredTime + triggerInterval
    if (currentProcessingTime < nextTriggerTime) {
      ctx.registerProcessingTimeTimer(nextTriggerTime)
    }

    if (updatedCount >= triggerCount) {
      countState.clear()
      lastFiredState.update(currentProcessingTime)
      TriggerResult.FIRE_AND_PURGE
    }
    else {
      TriggerResult.CONTINUE
    }
  }

  override def onProcessingTime(time: Long, window: GlobalWindow, ctx: Trigger.TriggerContext): TriggerResult = {
    val lastFiredState = getPartitionedState(ctx, "lastFiredTime")
    val lastFiredTime  = Option(lastFiredState.value()).getOrElse(0L)

    if (time >= lastFiredTime + triggerInterval) {
      val countState   = getPartitionedState(ctx, "count")
      val currentCount = Option(countState.value()).getOrElse(0L)

      if (currentCount > 0) {
        countState.clear()
        lastFiredState.update(time)
        TriggerResult.FIRE_AND_PURGE
      }
      else {
        TriggerResult.CONTINUE
      }
    }
    else {
      TriggerResult.CONTINUE
    }
  }

  override def onEventTime(time: Long, window: GlobalWindow, ctx: Trigger.TriggerContext): TriggerResult =
    TriggerResult.CONTINUE

  override def clear(window: GlobalWindow, ctx: Trigger.TriggerContext): Unit =
    List("count", "lastFiredTime").foreach { name =>
      getPartitionedState(ctx, name).clear()
    }
}

object CountOrTimerTrigger {

  def of(maxCount: Long, triggerDelay: Long): CountOrTimerTrigger =
    new CountOrTimerTrigger(maxCount, triggerDelay)
}
