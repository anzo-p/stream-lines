package com.anzop.appconfig

import com.typesafe.config.{Config, ConfigFactory}
import org.apache.flink.streaming.api.windowing.time.Time

import java.time.Duration

case class WatermarkConfig(dueTime: Duration, idlePatience: Duration)

case class SlidingWindowConfig(watermark: WatermarkConfig, windowPeriodLength: Time, windowInterval: Time)

object WindowConfig {
  val config: Config = ConfigFactory.load()

  val slidingWindows5m = SlidingWindowConfig(
    WatermarkConfig(
      dueTime      = config.getDuration("sliding_window_5min.watermark.due_time"),
      idlePatience = config.getDuration("sliding_window_5min.watermark.idle_patience")
    ),
    windowPeriodLength = Time.minutes(config.getDuration("sliding_window_5min.window_period_length").toMinutes),
    windowInterval     = Time.minutes(config.getDuration("sliding_window_5min.window_interval").toMinutes)
  )
}
