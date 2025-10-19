package net.anzop.sources.marketData

import net.anzop.config.{InfluxConfig, SourceRunnerConfig}
import net.anzop.helpers.DateAndTimeHelpers
import net.anzop.helpers.DateAndTimeHelpers.millisToMinutes
import net.anzop.models.MarketData
import org.apache.flink.streaming.api.functions.source.SourceFunction

import java.time.DayOfWeek

class MarketDataSource(influxConfig: InfluxConfig, config: SourceRunnerConfig) extends SourceFunction[MarketData] {
  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  @volatile private var running           = true
  @transient private var dbConn: InfluxDB = _

  private def checkAndSetTimer(runnable: Runnable): Unit = {
    val now       = DateAndTimeHelpers.nowAtNyse()
    val dayOfWeek = now.getDayOfWeek.getValue
    val hour      = now.getHour

    if (dayOfWeek <= DayOfWeek.FRIDAY.getValue &&
        hour >= config.dawn && hour <= config.dusk) {
      try {
        logger.info(s"Executing timed ${getClass.getName}.run task")
        runnable.run()
      } catch {
        case ex: Exception =>
          logger.warn(s"Error during task execution: ${ex.getMessage}")
      }
    }
  }

  private def fetchCollect(ctx: SourceFunction.SourceContext[MarketData]): Unit = {
    // delete trend measurement for a full redo; processors will know to recalculate everything
    val lastestTrendEntry: Option[Long] =
      dbConn
        .requestData[Long](
          mapping = GetLatestTrendEntry,
          params = QueryParams(
            bucket      = influxConfig.bucket,
            measurement = "trend"
          )
        )
        .headOption

    logger.info(s"Latest trend entry: $lastestTrendEntry")

    dbConn
      .requestData[MarketData](
        mapping = GetIndexData,
        params = QueryParams(
          bucket      = influxConfig.bucket,
          measurement = influxConfig.consumedMeasure,
          start       = lastestTrendEntry
        )
      )
      .foreach(ctx.collect)
  }

  override def run(ctx: SourceFunction.SourceContext[MarketData]): Unit =
    try {
      dbConn = new InfluxDB(influxConfig)
      logger.info(s"Executing initial ${getClass.getName}.run task")
      fetchCollect(ctx)

      while (running) {
        // wait immediately to prevent duplicates with init run
        val now          = DateAndTimeHelpers.nowAtNyse()
        val nextExecTime = now.plusMinutes(millisToMinutes(config.interval))
        Thread.sleep(java.time.Duration.between(now, nextExecTime).toMillis)

        logger.info(s"Executing timed ${getClass.getName}.run task")
        checkAndSetTimer(() => fetchCollect(ctx))
      }
    } catch {
      case ex: Exception =>
        logger.warn(s"Error in SourceFunction: ${ex.getMessage}")
    } finally {
      running = false
      if (dbConn != null) {
        dbConn.close()
      }
    }

  override def cancel(): Unit =
    running = false
}
