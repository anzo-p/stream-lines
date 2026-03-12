package net.anzop.config

import net.anzop.helpers.MapExtensions.EnvOps
import net.anzop.helpers.StreamHelpers.logger
import net.anzop.serdes.LocalJsonSerializer
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest

import java.net.URL
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

case class InfluxDbConfig(uri: URL, bucket: String, token: String)

object InfluxDbConfig extends LocalJsonSerializer {

  private def checkInfluxDB(): Unit = {
    implicit val ec = scala.concurrent.ExecutionContext.global
    val httpClient  = HttpClients.createDefault()
    val url         = sys.env.getOrThrow("INFLUXDB_URL", "INFLUXDB_URL is not set")
    val httpGet     = new HttpGet(url)

    val checkFuture = Future {
      val response   = httpClient.execute(httpGet)
      val statusCode = response.getStatusLine.getStatusCode
      if (statusCode != 200) {
        throw new RuntimeException(s"Failed to connect to InfluxDB: Received status code $statusCode")
      }

      logger.info("InfluxDB discovered")
      statusCode
    }

    Try(Await.result(checkFuture, 15.seconds)) match {
      case Success(_) =>
      case Failure(e) => {
        logger.info(s"Failed to connect to InfluxDB: ${e.getMessage}")
        throw new RuntimeException(s"Failed to connect to InfluxDB: ${e.getMessage}", e)
      }
    }
  }

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

  def make(): InfluxDbConfig = {
    checkInfluxDB()

    val baseUrl   = sys.env.getOrThrow("INFLUXDB_URL", "INFLUXDB_URL is not set")
    val org       = sys.env.getOrThrow("INFLUXDB_ORG", "INFLUXDB_ORG is not set")
    val bucket    = sys.env.getOrThrow("INFLUXDB_BUCKET_MARKET_DATA_REALTIME", "INFLUXDB_BUCKET_MARKET_DATA_REALTIME is not set")
    val token     = getSecret("prod/influxdb/market-data-realtime/write", "influxdb.token")
    val uriPrefix = s"$baseUrl/api/v2/write?org=$org"

    InfluxDbConfig(new URL(uriPrefix), bucket, token)
  }
}
