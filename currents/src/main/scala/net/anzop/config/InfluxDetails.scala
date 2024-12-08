package net.anzop.config

import net.anzop.helpers.Extensions.EnvOps

import java.net.URL

case class InfluxDetails(
    uri: URL,
    bucket: String,
    token: String,
    org: String
  ) extends Serializable

object InfluxDetails {

  def make(): InfluxDetails = {
    val baseUrl = sys.env.getOrThrow("INFLUXDB_URL", "INFLUXDB_URL is not set")
    val org     = sys.env.getOrThrow("INFLUXDB_ORG", "INFLUXDB_ORG is not set")
    val bucket  = sys.env.getOrThrow("INFLUXDB_DAILY_BARS_BUCKET", "INFLUXDB_BUCKET is not set")
    val token   = sys.env.getOrThrow("INFLUXDB_DAILY_BARS_TOKEN", "INFLUXDB_TOKEN is not set").replace("\"", "")

    InfluxDetails(new URL(baseUrl), bucket, token, org)
  }
}
