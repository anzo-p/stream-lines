package net.anzop.config

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

case class TrendConfig(
    flinkWindowCount: Int,
    flinkWindowInterval: Long,
    minimumWindow: Int,
    regressionSlopeThreshold: Double,
    regressionVarianceLimit: Double,
    tippingPointThreshold: Double
  )

object TrendConfig {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  private val config: Config = ConfigFactory.load()

  val values: TrendConfig = TrendConfig(
    flinkWindowCount         = config.getInt("trend_discovery.flink_window_count"),
    flinkWindowInterval      = config.getLong("trend_discovery.flink_window_interval"),
    minimumWindow            = config.getInt("trend_discovery.minimum_window"),
    regressionSlopeThreshold = config.getDouble("trend_discovery.regression_slope_threshold"),
    regressionVarianceLimit  = config.getDouble("trend_discovery.regression_variance_limit"),
    tippingPointThreshold    = config.getDouble("trend_discovery.tipping_point_threshold")
  )
  logger.info(s"trend discovery configuration loaded")
}
