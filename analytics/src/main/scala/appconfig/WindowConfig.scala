package appconfig

import com.typesafe.config.{Config, ConfigFactory}

import scala.concurrent.duration._

case class WatermarkConfig(dueTime: Duration, idlePatience: Duration)

case class SlidingWindowConfig(watermark: WatermarkConfig, windowPeriodLength: Duration, windowInterval: Duration)

object WindowConfig {
  val config: Config = ConfigFactory.load()

  val windodConfig = SlidingWindowConfig(
    WatermarkConfig(
      dueTime      = config.getDuration("sliding_window_5min.watermark.dueTime").toMinutes.minutes,
      idlePatience = config.getDuration("sliding_window_5min.watermark.idlePatience").toMinutes.minutes
    ),
    windowPeriodLength = config.getDuration("sliding_window_5min.windowPeriodLength").toMinutes.minutes,
    windowInterval     = config.getDuration("sliding_window_5min.windowInterval").toMinutes.minutes
  )
}
