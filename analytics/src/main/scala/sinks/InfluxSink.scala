package sinks

import appconfig.InfluxDetails
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import results.{DataSerializer, WindowedQuotationVolumes}

import java.io.IOException
import scala.util.Try

object InfluxBucket extends Enumeration {
  type Name = Value

  private case class BucketName(name: String) extends super.Val(nextId, name)

  val CryptoBucket: InfluxBucket.Value = BucketName("control-tower-crypto-bucket")
  val StockBucket: InfluxBucket.Value  = BucketName("control-tower-stock-bucket")
}

trait InfluxSink[T] extends RichSinkFunction[T] {
  lazy val httpClient: CloseableHttpClient = HttpClients.createDefault()

  def influxDetails: InfluxDetails
  val serializer: DataSerializer[T]
  val baseUri: String = s"${influxDetails.uri.toString}&bucket="
  val token: String   = influxDetails.token

  override def close(): Unit = {
    super.close()
    Try(httpClient.close()).recover {
      case e: IOException => println(s"Error closing HTTP client: ${e.getMessage}")
    }
  }
}

class ResultSink[T] private (val influxDetails: InfluxDetails, bucket: InfluxBucket.Name, val serializer: DataSerializer[T])
    extends InfluxSink[T] {
  override def invoke(value: T, context: SinkFunction.Context): Unit = {
    val serializedData = serializer.serialize(value)
    val httpPost       = new HttpPost(baseUri + bucket.toString)
    httpPost.setEntity(new StringEntity(serializedData))
    httpPost.addHeader("Authorization", s"Token $token")

    Try {
      val response = httpClient.execute(httpPost)
      response.getStatusLine.getStatusCode match {
        case 204 =>
        case _ =>
          println(
            s"Unexpected request from influx - code: ${response.getStatusLine.getStatusCode}, message: ${response.getStatusLine.getReasonPhrase}")
      }
    }.recover {
      case e: Exception => {
        println(s"error while sending data to influx: ${e.getMessage}")
      }
    }
  }
}

object ResultSink {

  def forCryptoQuotation(
      influxDetails: InfluxDetails,
      serializer: DataSerializer[WindowedQuotationVolumes]
    ): ResultSink[WindowedQuotationVolumes] =
    new ResultSink[WindowedQuotationVolumes](influxDetails, InfluxBucket.CryptoBucket, serializer)

  def forStockQuotation(
      influxDetails: InfluxDetails,
      serializer: DataSerializer[WindowedQuotationVolumes]
    ): ResultSink[WindowedQuotationVolumes] =
    new ResultSink[WindowedQuotationVolumes](influxDetails, InfluxBucket.StockBucket, serializer)
}
