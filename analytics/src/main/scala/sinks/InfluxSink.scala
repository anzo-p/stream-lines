package sinks

import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import processors.{InfluxdbResult, WindowedQuotationVolumes}

trait InfluxSink[T] extends RichSinkFunction[T] {
  final private val baseUrl = System.getenv().getOrDefault("INFLUXDB_URL", "http://localhost:8086")
  final private val org     = System.getenv().getOrDefault("INFLUXDB_ORG", "")
  final val token           = System.getenv().getOrDefault("INFLUXDB_TOKEN", "")
  final val uriPrefix       = s"$baseUrl/api/v2/write?org=$org"
}

class ResultSink[T <: InfluxdbResult](bucket: String) extends InfluxSink[T] {
  @transient private var httpClient: CloseableHttpClient = _

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    httpClient = HttpClients.createDefault()
  }

  override def close(): Unit = {
    super.close()
    if (httpClient != null) {
      httpClient.close()
    }
  }

  override def invoke(value: T, context: SinkFunction.Context): Unit = {
    val uri = List(uriPrefix, s"bucket=$bucket").mkString("&")

    try {
      val httpPost = new HttpPost(uri)
      httpPost.setEntity(new StringEntity(value.toLineProtocol))
      httpPost.addHeader("Authorization", s"Token $token")

      println(s"{${java.time.Instant.now()}} - making request to influx: $uri - ${value.toLineProtocol}")
      val response = httpClient.execute(httpPost)
      //println(s"{${java.time.Instant.now()}} - response from influx: ${response.getStatusLine.getStatusCode}")
      // handle response
    } catch {
      case e: Exception => {
        println(s"error while sending data to influx: ${e.getMessage}")
      }
    }
  }
}

object ResultSink {

  def forCryptoQuotation(): ResultSink[WindowedQuotationVolumes] =
    new ResultSink[WindowedQuotationVolumes]("control-tower-crypto-bucket")

  def forStockQuotation(): ResultSink[WindowedQuotationVolumes] =
    new ResultSink[WindowedQuotationVolumes]("control-tower-stock-bucket")
}
