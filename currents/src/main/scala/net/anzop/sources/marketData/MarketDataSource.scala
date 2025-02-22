package net.anzop.sources.marketData

import net.anzop.config.{InfluxConfig, RunConfig}
import net.anzop.models.MarketData
import org.apache.flink.streaming.api.functions.source.SourceFunction

import java.time.{DayOfWeek, LocalDateTime, ZoneId}

class MarketDataSource(influxConfig: InfluxConfig, runConfig: RunConfig) extends SourceFunction[MarketData] {
  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  @volatile private var running           = true
  @transient private var dbConn: InfluxDB = _

  private def checkAndSetTimer(runnable: Runnable): Unit = {
    val now           = LocalDateTime.now(ZoneId.of("UTC"))
    val dayOfWeek     = now.getDayOfWeek.getValue
    val hour          = now.getHour
    val nextExecTime  = now.plusHours(runConfig.interval - (hour % runConfig.interval))
    val delayNextExec = java.time.Duration.between(now, nextExecTime).toMillis
    val delayNextTry  = java.time.Duration.between(now, now.plusMinutes(10)).toMillis

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
      Thread.sleep(delayNextExec)
    }
    else {
      Thread.sleep(delayNextTry)
    }
  }

  private def fetchCollect(ctx: SourceFunction.SourceContext[MarketData]): Unit = {
    val lastTrendEnding: Option[Long] =
      dbConn
        .requestData[Long](
          mapping = LatestTrendEnding,
          params  = QueryParams(influxConfig.bucket, influxConfig.trendMeasure)
        )
        .headOption

    dbConn
      .requestData[MarketData](
        mapping = IndexData,
        params = QueryParams(
          bucket      = influxConfig.bucket,
          measurement = influxConfig.indexMeasure,
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
