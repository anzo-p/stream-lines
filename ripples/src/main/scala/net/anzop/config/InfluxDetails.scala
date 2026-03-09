package net.anzop.config

import net.anzop.helpers.MapExtensions.EnvOps
import net.anzop.serdes.LocalJsonSerializer
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

import java.net.URL

case class InfluxDetails(uri: URL, bucket: String, token: String)

object InfluxDetails extends LocalJsonSerializer {

  private def getSecret(secretName: String, key: String): String = {
    val request = GetSecretValueRequest
      .builder()
      .secretId(secretName)
      .build()

    val json = SecretsManagerClient
      .create()
      .getSecretValue(request)
      .secretString()

    jsonValue(json, "influxdb.token")
      .getOrElse(throw new RuntimeException(s"Key '$key' not found in secret '$secretName'"))
  }

  def make(): InfluxDetails = {
    val baseUrl   = sys.env.getOrThrow("INFLUXDB_URL", "INFLUXDB_URL is not set")
    val org       = sys.env.getOrThrow("INFLUXDB_ORG", "INFLUXDB_ORG is not set")
    val bucket    = sys.env.getOrThrow("INFLUXDB_BUCKET_MARKET_DATA_REALTIME", "INFLUXDB_BUCKET_MARKET_DATA_REALTIME is not set")
    val token     = getSecret("prod/influxdb/market-data-realtime/write", "influxdb.token")
    val uriPrefix = s"$baseUrl/api/v2/write?org=$org"

    InfluxDetails(new URL(uriPrefix), bucket, token)
  }
}
