package net.anzop.sinks.influxdb

import net.anzop.config.InfluxConfig
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}

import java.io.IOException
import scala.util.Try

class InfluxHttpSink[T <: InfluxSerializable](val influxDetails: InfluxConfig) extends InfluxBaseSink[T] {
  private lazy val httpClient: CloseableHttpClient = HttpClients.createDefault()

  private val baseUri: String = s"${influxDetails.sinkUrl.toString}&bucket="
  private val token: String   = influxDetails.writeToken

  override def send(serialized: String): Unit = {
    val httpPost = new HttpPost(baseUri + influxDetails.bucket)
    httpPost.setEntity(new StringEntity(serialized))
    httpPost.addHeader("Authorization", s"Token $token")

    Try {
      val response = httpClient.execute(httpPost)
      response.getStatusLine.getStatusCode match {
        case 204 =>
          logger.info(s"Successfully stored data in InfluxDB - $serialized")
        case _ =>
          logger.warn(
            s"Unexpected response from InfluxDB - code: ${response.getStatusLine.getStatusCode}, " +
              s"message: ${response.getStatusLine.getReasonPhrase}"
          )
      }
    }.recover {
      case e: Exception =>
        logger.error(s"Error while sending data to InfluxDB: ${e.getMessage}")
    }
  }

  override def close(): Unit =
    Try(httpClient.close()).recover {
      case e: IOException => logger.error(s"Error closing HTTP client: ${e.getMessage}")
    }
}
