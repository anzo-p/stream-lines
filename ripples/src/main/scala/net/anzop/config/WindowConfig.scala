package net.anzop.config

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.flink.streaming.api.windowing.time.Time
import org.slf4j.{Logger, LoggerFactory}

import java.time.Duration

case class WatermarkConfig(dueTime: Duration, idlePatience: Duration)

case class SlidingWindowConfig(watermark: WatermarkConfig, windowPeriodLength: Time, windowInterval: Time)

object WindowConfig {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  private val config: Config     = ConfigFactory.load()
  private val periodLength: Long = config.getDuration("sliding_window_5min.window_period_length").toMinutes

  val slidingWindows5m = SlidingWindowConfig(
    WatermarkConfig(
      dueTime      = config.getDuration("sliding_window_5min.watermark.due_time"),
      idlePatience = config.getDuration("sliding_window_5min.watermark.idle_patience")
    ),
    windowPeriodLength = Time.minutes(periodLength),
    windowInterval     = Time.minutes(config.getDuration("sliding_window_5min.window_interval").toMinutes)
  )
  logger.info(s"$periodLength minute sliding window configuration loaded")
}
