package net.anzop.config

import net.anzop.helpers.Extensions.EnvOps

import java.net.URL

case class InfluxDetails(uri: URL, bucket: String, token: String)

object InfluxDetails {

  def make(): InfluxDetails = {
    val baseUrl   = sys.env.getOrThrow("INFLUXDB_URL", "INFLUXDB_URL is not set")
    val org       = sys.env.getOrThrow("INFLUXDB_ORG", "INFLUXDB_ORG is not set")
    val bucket    = sys.env.getOrThrow("INFLUXDB_BUCKET_MARKET_DATA_REALTIME", "INFLUXDB_BUCKET_MARKET_DATA_REALTIME is not set")
    val token     = sys.env.getOrThrow("INFLUXDB_TOKEN_REALTIME_WRITE", "INFLUXDB_TOKEN_REALTIME_WRITE is not set").replace("\"", "")
    val uriPrefix = s"$baseUrl/api/v2/write?org=$org"

    InfluxDetails(new URL(uriPrefix), bucket, token)
  }
}
