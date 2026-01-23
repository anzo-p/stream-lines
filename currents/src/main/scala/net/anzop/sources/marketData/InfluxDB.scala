package net.anzop.sources.marketData

import com.influxdb.client.domain.DeletePredicateRequest
import com.influxdb.client.{InfluxDBClient, InfluxDBClientFactory}
import net.anzop.config.InfluxConfig

import java.io.Serializable
import java.time.OffsetDateTime
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

  def deleteOpenTrendSegments(): Unit = {
    try {
      client
        .getDeleteApi
        .delete(
          new DeletePredicateRequest()
            .start(OffsetDateTime.parse("1900-01-01T00:00:00Z"))
            .stop(OffsetDateTime.parse("2199-12-31T00:00:00Z"))
            .predicate(
              """_measurement="trends-by-statistical-regression" AND established == "true" """
            ),
          influxDetails.bucket,
          influxDetails.org
        )
    } catch {
      case e: Exception =>
        logger.error(s"Failed to delete data from InfluxDB: $e")
    }
  }

  def close(): Unit =
    client.close()
}
