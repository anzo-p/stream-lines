package net.anzop.helpers

import MapExtensions.EnvOps
import net.anzop.types.{MarketDataContent, MarketDataMessage}
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala._
import org.apache.http.client.methods.HttpGet
import org.apache.http.impl.client.HttpClients
import org.slf4j.{Logger, LoggerFactory}

import java.time.{Duration, ZoneId, ZonedDateTime}
import scala.concurrent.duration.DurationInt
import scala.concurrent.{Await, Future}
import scala.reflect.ClassTag
import scala.util.{Failure, Success, Try}

object StreamHelpers {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  def checkInfluxDB(): Unit = {
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

  def nyseOpen(): Long = {
    val zoneNy = ZoneId.of("America/New_York")
    val openNy = ZonedDateTime.now(zoneNy).toLocalDate.atTime(9, 30).atZone(zoneNy)
    openNy.minusHours(1).toInstant.toEpochMilli
  }

  def filterType[T <: MarketDataContent : ClassTag : TypeInformation](stream: DataStream[MarketDataMessage]): DataStream[T] = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    stream
      .filter(_.messageType.getClass.isAssignableFrom(clazz))
      .map(_.messageType.asInstanceOf[T])(implicitly[TypeInformation[T]])
  }

  def watermarkForBound[T <: MarketDataContent](stream: DataStream[T], dueTime: Duration, idlePatience: Duration): DataStream[T] =
    stream
      .assignTimestampsAndWatermarks(
        WatermarkStrategy
          .forBoundedOutOfOrderness[T](dueTime)
          .withTimestampAssigner(
            new SerializableTimestampAssigner[T] {
              override def extractTimestamp(element: T, recordTimestamp: Long): Long =
                element.marketTimestamp.toInstant.toEpochMilli
            }
          )
          .withIdleness(idlePatience)
      )
}
