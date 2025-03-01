package net.anzop.config

import net.anzop.helpers.Extensions.EnvOps

import java.net.URL

case class InfluxConfig(
    sourceUrl: URL,
    sinkUrl: URL,
    org: String,
    readToken: String,
    writeToken: String,
    bucket: String,
    consumedMeasure: String
  ) extends Serializable

object InfluxConfig {

  val values: InfluxConfig = {
    val baseUrl = sys.env.getOrThrow("INFLUXDB_URL", "INFLUXDB_URL is not set")
    val org     = sys.env.getOrThrow("INFLUXDB_ORG", "INFLUXDB_ORG is not set")

    InfluxConfig(
      sourceUrl       = new URL(baseUrl),
      sinkUrl         = new URL(s"$baseUrl/api/v2/write?org=$org"),
      org             = org,
      readToken       = sys.env.getOrThrow("INFLUXDB_READ_TOKEN", "INFLUXDB_READ_TOKEN is not set").replace("\"", ""),
      writeToken      = sys.env.getOrThrow("INFLUXDB_WRITE_TOKEN", "INFLUXDB_WRITE_TOKEN is not set").replace("\"", ""),
      bucket          = sys.env.getOrThrow("INFLUXDB_BUCKET", "INFLUXDB_BUCKET is not set"),
      consumedMeasure = sys.env.getOrThrow("INFLUXDB_CONSUME_MEASURE", "INFLUXDB_CONSUME_MEASURE is not set")
    )
  }
}
