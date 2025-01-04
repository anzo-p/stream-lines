package net.anzop.config

import net.anzop.helpers.Extensions.EnvOps

import java.net.URL

case class InfluxConfig(
    sourceUrl: URL,
    sinkUrl: URL,
    org: String,
    token: String,
    bucket: String,
    indexMeasure: String,
    trendMeasure: String,
    drawdownMeasure: String
  ) extends Serializable

object InfluxConfig {

  val values: InfluxConfig = {
    val baseUrl = sys.env.getOrThrow("INFLUXDB_URL", "INFLUXDB_URL is not set")
    val org     = sys.env.getOrThrow("INFLUXDB_ORG", "INFLUXDB_ORG is not set")

    InfluxConfig(
      sourceUrl       = new URL(baseUrl),
      sinkUrl         = new URL(s"$baseUrl/api/v2/write?org=$org"),
      org             = org,
      token           = sys.env.getOrThrow("INFLUXDB_DAILY_BARS_TOKEN", "INFLUXDB_TOKEN is not set").replace("\"", ""),
      bucket          = sys.env.getOrThrow("INFLUXDB_DAILY_BARS_BUCKET", "INFLUXDB_BUCKET is not set"),
      indexMeasure    = sys.env.getOrThrow("INFLUXDB_MEASURE_INDEX", "INFLUXDB_MEASURE_INDEX is not set"),
      trendMeasure    = sys.env.getOrThrow("INFLUXDB_MEASURE_TREND", "INFLUXDB_MEASURE_TREND is not set"),
      drawdownMeasure = sys.env.getOrThrow("INFLUXDB_MEASURE_DRAWDOWN", "INFLUXDB_MEASURE_DRAWDOWN is not set")
    )
  }
}
