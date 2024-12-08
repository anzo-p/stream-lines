package net.anzop.config

import com.typesafe.config.{Config, ConfigFactory}
import org.slf4j.{Logger, LoggerFactory}

case class TrendDiscoveryConfig(
    minimumWindow: Int,
    regressionSlopeThreshold: Double,
    regressionVarianceThreshold: Double,
    tippingPointThreshold: Double
  )

object TrendDiscoveryConfig {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  private val config: Config = ConfigFactory.load()

  val values = TrendDiscoveryConfig(
    minimumWindow               = config.getInt("trend_discovery.minimum_window"),
    regressionSlopeThreshold    = config.getDouble("trend_discovery.regression_slope_threshold"),
    regressionVarianceThreshold = config.getDouble("trend_discovery.regression_variance_threshold"),
    tippingPointThreshold       = config.getDouble("trend_discovery.tipping_point_threshold")
  )
  logger.info(s"trend discovery configuration loaded")
}
