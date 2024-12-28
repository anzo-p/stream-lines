package net.anzop.sources.marketData

import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory}
import net.anzop.config.InfluxDetails

import java.io.Serializable
import java.time.Instant
import scala.jdk.CollectionConverters._

case class QueryParams(
    bucket: String,
    measurement: String,
    start: Option[Long] = Some(1L),
    stop: Option[Long]  = Some(Instant.now().getEpochSecond)
  )

class InfluxDB(influxDetails: InfluxDetails) extends Serializable {
  private val logger = org.slf4j.LoggerFactory.getLogger(getClass)

  private val client: InfluxDBClient = InfluxDBClientFactory.create(
    influxDetails.sourceUrl.toString,
    influxDetails.token.toCharArray,
    influxDetails.org
  )

  private val queryApi = client.getQueryApi

  def requestData[T](fetchConfig: DatasetMapping[T], params: QueryParams): List[T] = {
    logger.info(s"Fetching data from InfluxDB for $fetchConfig with params: $params")

    try {
      queryApi
        .query(fetchConfig.query(params), influxDetails.org)
        .asScala
        .toList
        .flatMap(fetchConfig.tableMapper)
    } catch {
      case e: Exception => {
        logger.error(s"Failed to fetch data from InfluxDB: $e")
        List.empty
      }
    }
  }

  def close(): Unit =
    client.close()
}
