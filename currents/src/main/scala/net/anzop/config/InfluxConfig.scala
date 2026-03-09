package net.anzop.config

import net.anzop.helpers.Extensions.EnvOps
import net.anzop.serdes.LocalJsonSerializer
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

import java.net.URL

case class InfluxConfig(
    sourceUrl: URL,
    sinkUrl: URL,
    org: String,
    readWriteToken: String,
    bucket: String,
    consumedMeasure: String
  ) extends Serializable

object InfluxConfig extends LocalJsonSerializer {

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

  val values: InfluxConfig = {
    val baseUrl = sys.env.getOrThrow("INFLUXDB_URL", "INFLUXDB_URL is not set")
    val org     = sys.env.getOrThrow("INFLUXDB_ORG", "INFLUXDB_ORG is not set")

    InfluxConfig(
      sourceUrl      = new URL(baseUrl),
      sinkUrl        = new URL(s"$baseUrl/api/v2/write?org=$org"),
      org            = org,
      readWriteToken = getSecret("prod/influxdb/market-data-historical/read-write", "influxdb.token"),
      bucket = sys
        .env
        .getOrThrow("INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL", "INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL is not set"),
      consumedMeasure = sys
        .env
        .getOrThrow("INFLUXDB_CONSUME_MEASURE", "INFLUXDB_CONSUME_MEASURE is not set")
    )
  }
}
