package net.anzop.config

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

import scala.concurrent.duration.Duration

case class SourceRunnerConfig(dawn: Int, dusk: Int, interval: Long)

object SourceRunnerConfig {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  private val config: Config = ConfigFactory.load()

  val values: SourceRunnerConfig = {
    val nyse: Config        = config.getConfig("source.runner.nyse.extended.hours")
    val intervalStr: String = config.getString("source.runner.run.interval")

    SourceRunnerConfig(
      dawn     = nyse.getInt("before.open"),
      dusk     = nyse.getInt("after.close"),
      interval = Duration(intervalStr).toMillis
    )
  }

  logger.info(s"Source runner configuration loaded")
}
