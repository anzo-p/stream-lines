package net.anzop.sinks

import net.anzop.config.InfluxConfig
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.slf4j.{Logger, LoggerFactory}

import java.io.IOException
import scala.util.Try

trait InfluxSink[T] extends RichSinkFunction[T] {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  lazy val httpClient: CloseableHttpClient = HttpClients.createDefault()

  def influxDetails: InfluxConfig
  val serializer: DataSerializer[T]
  val baseUri: String = s"${influxDetails.sinkUrl.toString}&bucket="
  val token: String   = influxDetails.token

  override def close(): Unit = {
    super.close()
    Try(httpClient.close()).recover {
      case e: IOException => logger.error(s"Error closing HTTP client: ${e.getMessage}")
    }
  }
}

class ResultSink[T](val influxDetails: InfluxConfig, val serializer: DataSerializer[T]) extends InfluxSink[T] {
  override def invoke(value: T, context: SinkFunction.Context): Unit = {
    val serializedData = serializer.serialize(value)
    if (serializedData.isBlank) {
      logger.warn("Empty data received, skipping")
      return
    }

    val httpPost = new HttpPost(baseUri + influxDetails.bucket)
    httpPost.setEntity(new StringEntity(serializedData))
    httpPost.addHeader("Authorization", s"Token $token")

    Try {
      val response = httpClient.execute(httpPost)
      response.getStatusLine.getStatusCode match {
        case 204 =>
        //logger.info("Successfully stored data in InfluxDB")
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
}
