package net.anzop.config

import net.anzop.helpers.Extensions.EnvOps

import java.net.URL

case class InfluxDetails(
    sourceUrl: URL,
    sinkUrl: URL,
    org: String,
    token: String,
    bucket: String,
    indexMeasure: String,
    trendMeasure: String,
    drawdownMeasure: String
  ) extends Serializable

object InfluxDetails {

  def make(): InfluxDetails = {
    val baseUrl         = sys.env.getOrThrow("INFLUXDB_URL", "INFLUXDB_URL is not set")
    val org             = sys.env.getOrThrow("INFLUXDB_ORG", "INFLUXDB_ORG is not set")
    val token           = sys.env.getOrThrow("INFLUXDB_DAILY_BARS_TOKEN", "INFLUXDB_TOKEN is not set").replace("\"", "")
    val bucket          = sys.env.getOrThrow("INFLUXDB_DAILY_BARS_BUCKET", "INFLUXDB_BUCKET is not set")
    val indexMeasure    = sys.env.getOrThrow("INFLUXDB_MEASURE_INDEX", "INFLUXDB_MEASURE_INDEX is not set")
    val trendMeasure    = sys.env.getOrThrow("INFLUXDB_MEASURE_TREND", "INFLUXDB_MEASURE_TREND is not set")
    val drawdownMeasure = sys.env.getOrThrow("INFLUXDB_MEASURE_DRAWDOWN", "INFLUXDB_MEASURE_DRAWDOWN is not set")
    val sinkUrl         = s"$baseUrl/api/v2/write?org=$org"

    InfluxDetails(new URL(baseUrl), new URL(sinkUrl), org, token, bucket, indexMeasure, trendMeasure, drawdownMeasure)
  }
}
