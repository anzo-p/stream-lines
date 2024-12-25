package net.anzop.sources.marketData

import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory}
import net.anzop.config.InfluxDetails

import java.io.Serializable
import scala.jdk.CollectionConverters._

class InfluxDB(influxDetails: InfluxDetails) extends Serializable {
  private val client: InfluxDBClient = InfluxDBClientFactory.create(
    influxDetails.sourceUrl.toString,
    influxDetails.token.toCharArray,
    influxDetails.org
  )

  private val queryApi = client.getQueryApi

  def requestData[T](fetchConfig: DatasetMapping[T]): List[T] =
    try {
      queryApi
        .query(fetchConfig.query, influxDetails.org)
        .asScala
        .toList
        .flatMap(fetchConfig.mapper)
    } catch {
      case _: Exception => List.empty
    }

  def close(): Unit =
    client.close()
}
