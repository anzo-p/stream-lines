package net.anzop.sources.marketData

import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory}
import net.anzop.config.InfluxConfig

import java.io.Serializable
import scala.jdk.CollectionConverters._

class InfluxDB(influxDetails: InfluxConfig) extends Serializable {
  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  private val client: InfluxDBClient = InfluxDBClientFactory.create(
    influxDetails.sourceUrl.toString,
    influxDetails.readToken.toCharArray,
    influxDetails.org
  )

  private val queryApi = client.getQueryApi

  def requestData[T](mapping: DatasetMapping[T], params: QueryParams): List[T] =
    try {
      queryApi
        .query(mapping.query(params), influxDetails.org)
        .asScala
        .toList
        .flatMap(mapping.tableMapper)
    } catch {
      case e: Exception =>
        logger.error(s"Failed to fetch data from InfluxDB: $e")
        List.empty
    }

  def close(): Unit =
    client.close()
}
