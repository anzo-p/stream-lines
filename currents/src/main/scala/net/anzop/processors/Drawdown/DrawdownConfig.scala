package net.anzop.processors.Drawdown

import com.typesafe.config.{Config, ConfigFactory}
import net.anzop.helpers.DateAndTimeHelpers.dateToUtcMidnight
import org.slf4j.{Logger, LoggerFactory}

import java.time.Instant

case class DrawdownConfig(earliestHistoricalDate: Instant)

object DrawdownConfig {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  private val config: Config = ConfigFactory.load()

  val values: DrawdownConfig = DrawdownConfig(dateToUtcMidnight(config.getString("alpaca.earliest_historical_date")))

  logger.info(s"Drawdown configuration loaded")
}
