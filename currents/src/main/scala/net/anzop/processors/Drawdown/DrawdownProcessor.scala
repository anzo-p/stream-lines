package net.anzop.processors.Drawdown

import net.anzop.helpers.DateAndTimeHelpers.isBeforeToday
import net.anzop.models.MarketData
import net.anzop.processors.Drawdown.DynamoDbMapper._
import net.anzop.repository.dynamodb.DynamoDb
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector

import scala.util.chaining._

class DrawdownProcessor(config: DrawdownConfig) extends KeyedProcessFunction[String, MarketData, Drawdown] {
  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  private val initState: (Long, Double)                 = (0L, 0.0)
  private val saveDelay: Long                           = 15 * 1000L
  private var maxValueState: ValueState[(Long, Double)] = _
  private var timerState: ValueState[Long]              = _

  private def getOrFetchState(): (Long, Double) =
    Option(maxValueState.value()) match {
      case Some(state) => state
      case None =>
        DynamoDb.getSingle match {
          case Some(state) =>
            logger.info(s"Restoring state from DynamoDB: $state")
            state
          case _ =>
            logger.info("No state found, even in DynamoDB - initializing with starting state")
            initState
        }
    }

  private def resolveState(ts: Long): (Long, Double) =
    if (ts <= config.earliestHistoricalDate.toEpochMilli) {
      logger.info(s"Data elements indicates a redo from beginning - initializing with starting state")
      initState
    }
    else {
      getOrFetchState().tap(maxValueState.update)
    }

  override def open(parameters: Configuration): Unit = {
    maxValueState = getRuntimeContext.getState(
      new ValueStateDescriptor[(Long, Double)]("maxValueState", classOf[(Long, Double)])
    )
    timerState = getRuntimeContext.getState(
      new ValueStateDescriptor[Long]("timerState", classOf[Long])
    )
  }

  override def processElement(
      elem: MarketData,
      ctx: KeyedProcessFunction[String, MarketData, Drawdown]#Context,
      out: Collector[Drawdown]
    ): Unit = {
    val (lastTs, lastMaxValue) = resolveState(elem.timestamp)

    if (lastTs > initState._1 && elem.timestamp <= lastTs) {
      logger.info(s"Skipping out-of-order event: $elem (latest timestamp: $lastTs)")
      return
    }

    val updatedMaxValue = Math.max(lastMaxValue, elem.value)
    val drawdown        = (elem.value / updatedMaxValue) * 100

    if (isBeforeToday(elem.timestamp)) {
      val state = elem.timestamp -> updatedMaxValue
      maxValueState.update(state)

      Option(timerState.value()).foreach(ctx.timerService().deleteProcessingTimeTimer)

      val newTimer = ctx.timerService().currentProcessingTime() + saveDelay
      ctx.timerService().registerProcessingTimeTimer(newTimer)
      timerState.update(newTimer)
      logger.info(s"Scheduled new timer at $newTimer for state $state")
    }

    out.collect(
      Drawdown(
        elem.timestamp,
        elem.field,
        elem.value,
        drawdown
      )
    )
  }

  override def onTimer(
      timestamp: Long,
      ctx: KeyedProcessFunction[String, MarketData, Drawdown]#OnTimerContext,
      out: Collector[Drawdown]
    ): Unit =
    maxValueState.value() match {
      case null =>
      case state =>
        logger.info(s"Timer fired at $timestamp, saving state $state to DynamoDB")
        DynamoDb.save(state)
        timerState.clear()
    }
}
