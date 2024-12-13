package net.anzop.config

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

case class TrendConfig(
    flinkWindowCount: Int,
    flinkWindowInterval: Long,
    minimumTrendWindow: Int,
    regressionSlopeThreshold: Double,
    regressionVarianceThreshold: Double,
    tippingPointThreshold: Double
  )

object TrendConfig {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  private val config: Config = ConfigFactory.load()

  val values: TrendConfig = TrendConfig(
    flinkWindowCount            = config.getInt("trend_discovery.flink_window_count"),
    flinkWindowInterval         = config.getLong("trend_discovery.flink_window_interval"),
    minimumTrendWindow          = config.getInt("trend_discovery.minimum_trend_window"),
    regressionSlopeThreshold    = config.getDouble("trend_discovery.regression_slope_threshold"),
    regressionVarianceThreshold = config.getDouble("trend_discovery.regression_variance_threshold"),
    tippingPointThreshold       = config.getDouble("trend_discovery.tipping_point_threshold")
  )
  logger.info(s"trend discovery configuration loaded")
}
