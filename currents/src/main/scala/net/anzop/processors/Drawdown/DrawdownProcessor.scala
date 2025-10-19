package net.anzop.processors.Drawdown

import net.anzop.helpers.DateAndTimeHelpers.isBeforeToday
import net.anzop.models.MarketData
import net.anzop.processors.Drawdown.DynamoDbMapper._
import net.anzop.processors.AutoResettingProcessor
import net.anzop.repository.dynamodb.DynamoDb
import org.apache.flink.api.common.state.{ValueState, ValueStateDescriptor}
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.KeyedProcessFunction
import org.apache.flink.util.Collector
import org.slf4j.Logger

import scala.util.chaining._

class DrawdownProcessor(config: DrawdownConfig)
    extends KeyedProcessFunction[String, MarketData, Drawdown]
    with AutoResettingProcessor {

  private val logger: Logger = org.slf4j.LoggerFactory.getLogger(getClass)

  private var maxValuesState: ValueState[(Long, (Double, Double, Double))] = _
  private var timerState: ValueState[Long]                                 = _
  private val initState: (Long, (Double, Double, Double))                  = (0L, (0.0, 0.0, 0.0))
  private val saveDelay: Long                                              = 15 * 1000L

  private def resolveState(): (Long, (Double, Double, Double)) =
    Option(maxValuesState.value()) match {
      case Some(state) => state
      case None =>
        DynamoDb.getSingle match {
          case Some(state) =>
            logger.info(s"Drawdown - Restoring state from DynamoDB: $state")
            state
          case _ =>
            logger.info("Drawdown - No state found, even in DynamoDB")
            initState
        }
    }

  override val earliestExpectedElemTimestamp: Long = config.earliestHistoricalDate.toEpochMilli

  override def resetOp: () => Unit = () => {
    maxValuesState.clear()
    timerState.clear()
  }

  override def open(parameters: Configuration): Unit = {
    maxValuesState = getRuntimeContext.getState(
      new ValueStateDescriptor[(Long, (Double, Double, Double))](
        "maxValueState",
        classOf[(Long, (Double, Double, Double))])
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
    val (lastTs, (highestLow, highestAvg, highestHigh)) =
      if (autoResetState(elem.timestamp)) {
        logger.info(s"Drawdown - Data indicates reset; clearing state")
        initState
      }
      else {
        resolveState().tap(maxValuesState.update)
      }

    if (lastTs > initState._1 && elem.timestamp <= lastTs) {
      logger.info(s"Drawdown - Skipping out-of-order event: $elem (latest timestamp: $lastTs)")
      return
    }

    val highestLowUpdate  = Math.max(highestLow, elem.priceChangeLow)
    val highestAvgUpdate  = Math.max(highestAvg, elem.priceChangeAvg)
    val highestHighUpdate = Math.max(highestHigh, elem.priceChangeHigh)

    if (isBeforeToday(elem.timestamp)) {
      val state = elem.timestamp -> (highestLowUpdate, highestAvgUpdate, highestHighUpdate)
      maxValuesState.update(state)

      Option(timerState.value()).foreach(ctx.timerService().deleteProcessingTimeTimer)

      val newTimer = ctx.timerService().currentProcessingTime() + saveDelay
      ctx.timerService().registerProcessingTimeTimer(newTimer)
      timerState.update(newTimer)
    }

    out.collect(
      Drawdown(
        timestamp       = elem.timestamp,
        priceChangeLow  = elem.priceChangeLow,
        priceChangeAvg  = elem.priceChangeAvg,
        priceChangeHigh = elem.priceChangeHigh,
        drawdownLow     = (elem.priceChangeLow / highestLowUpdate) * 100,
        drawdownAvg     = (elem.priceChangeAvg / highestAvgUpdate) * 100,
        drawdownHigh    = (elem.priceChangeHigh / highestHighUpdate) * 100
      )
    )
  }

  override def onTimer(
      timestamp: Long,
      ctx: KeyedProcessFunction[String, MarketData, Drawdown]#OnTimerContext,
      out: Collector[Drawdown]
    ): Unit =
    maxValuesState.value() match {
      case null =>
      case state =>
        logger.info(s"Drawdown - Timer to save state: $state to DynamoDB fired at $timestamp")
        DynamoDb.save(state)
        timerState.clear()
    }
}
