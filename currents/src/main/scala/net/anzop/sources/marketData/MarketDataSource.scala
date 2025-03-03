package net.anzop.sources.marketData

import net.anzop.config.{InfluxConfig, RunConfig}
import net.anzop.helpers.DateAndTimeHelpers.millisToMinutes
import net.anzop.models.MarketData
import org.apache.flink.streaming.api.functions.source.SourceFunction

import java.time.{DayOfWeek, LocalDateTime, ZoneId}

class MarketDataSource(influxConfig: InfluxConfig, runConfig: RunConfig) extends SourceFunction[MarketData] {
  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  @volatile private var running           = true
  @transient private var dbConn: InfluxDB = _

  private def checkAndSetTimer(runnable: Runnable): Unit = {
    val now          = LocalDateTime.now(ZoneId.of("America/New_York")) // NYSE
    val dayOfWeek    = now.getDayOfWeek.getValue
    val hour         = now.getHour
    val nextExecTime = now.plusMinutes(millisToMinutes(runConfig.interval))

    if (dayOfWeek <= DayOfWeek.FRIDAY.getValue &&
        hour >= runConfig.dawn && hour <= runConfig.dusk &&
        hour % runConfig.interval == 0) {
      try {
        logger.info(s"Executing timed ${getClass.getName}.run task")
        runnable.run()
      } catch {
        case ex: Exception =>
          logger.warn(s"Error during task execution: ${ex.getMessage}")
      }
    }
    Thread.sleep(java.time.Duration.between(now, nextExecTime).toMillis)
  }

  private def fetchCollect(ctx: SourceFunction.SourceContext[MarketData]): Unit = {
    val lastTrendEnding: Option[Long] =
      dbConn
        .requestData[Long](
          mapping = LatestTrendEnding,
          params = QueryParams(
            bucket      = influxConfig.bucket,
            measurement = "trend"
          )
        )
        .headOption

    dbConn
      .requestData[MarketData](
        mapping = IndexData,
        params = QueryParams(
          bucket      = influxConfig.bucket,
          measurement = influxConfig.consumedMeasure,
          start       = lastTrendEnding
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
