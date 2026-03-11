package net.anzop.sinks

import net.anzop.config.InfluxDetails
import net.anzop.serdes.DataSerializer
import org.apache.flink.configuration.Configuration
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity
import org.apache.http.impl.client.{CloseableHttpClient, HttpClients}
import org.slf4j.{Logger, LoggerFactory}

import java.io.IOException
import java.util.concurrent.atomic.LongAdder
import java.util.concurrent.{ConcurrentHashMap, Executors, ScheduledExecutorService, ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}
import scala.jdk.CollectionConverters.asScalaSetConverter
import scala.util.Try

trait BaseInfluxSink[T] extends RichSinkFunction[T] {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  implicit def influxSerializer: DataSerializer[T]

  lazy val httpClient: CloseableHttpClient = HttpClients.createDefault()

  def influxDetails: InfluxDetails
  val baseUri: String = s"${influxDetails.uri.toString}&bucket="
  val token: String   = influxDetails.token

  override def close(): Unit = {
    super.close()
    Try(httpClient.close()).recover {
      case e: IOException => logger.error(s"Error closing HTTP client: ${e.getMessage}")
    }
  }
}

class InfluxSink[T](val influxDetails: InfluxDetails)(implicit val influxSerializer: DataSerializer[T]) extends BaseInfluxSink[T] {
  @transient private var scheduler: ScheduledExecutorService                     = Executors.newSingleThreadScheduledExecutor()
  @transient private var measurementCounts: ConcurrentHashMap[String, LongAdder] = new ConcurrentHashMap[String, LongAdder]()
  @transient @volatile private var scheduledFlush: ScheduledFuture[_]            = _
  private val debounceSeconds: Long                                              = 10L

  override def close(): Unit = {
    try {
      flushAggregates()
    } finally {
      cancelScheduledFlush()
      scheduler.shutdownNow()
      super.close()
    }
  }

  override def invoke(value: T, context: SinkFunction.Context): Unit = {
    val serializedData = influxSerializer.serialize(value)
    val httpPost       = new HttpPost(baseUri + influxDetails.bucket)
    httpPost.setEntity(new StringEntity(serializedData))
    httpPost.addHeader("Authorization", s"Token $token")

    Try {
      val response = httpClient.execute(httpPost)
      response.getStatusLine.getStatusCode match {
        case 204 =>
          val key = value.getClass.getSimpleName
          measurementCounts.computeIfAbsent(key, _ => new LongAdder()).increment()
          scheduleAggregateLogEntry()

        case 400 =>
          logger.error(s"Bad request sent to influxDB: $serializedData. ${response.getStatusLine.getReasonPhrase}")

        case _ =>
          logger.warn(
            s"Unexpected response from influx - code: ${response.getStatusLine.getStatusCode}," +
              s"message: ${response.getStatusLine.getReasonPhrase}"
          )
      }
    }.recover {
      case e: Exception => {
        logger.error(s"Error while sending data to influx: ${e.getMessage}")
      }
    }
  }

  override def open(parameters: Configuration): Unit = {
    super.open(parameters)
    scheduler         = new ScheduledThreadPoolExecutor(1)
    measurementCounts = new ConcurrentHashMap[String, LongAdder]()
  }

  private def cancelScheduledFlush(): Unit = this.synchronized {
    if (scheduledFlush != null && !scheduledFlush.isDone) {
      scheduledFlush.cancel(false)
    }
  }

  private def flushAggregates(): Unit = {
    val snapshot = measurementCounts
      .entrySet()
      .asScala
      .map(e => e.getKey -> e.getValue.sum())
      .toList

    if (snapshot.nonEmpty) {
      snapshot.foreach {
        case (name, count) =>
          logger.info(s"Successfully storing count: $count type: $name results to influxDB")
      }

      measurementCounts.clear()
    }
  }

  private def scheduleAggregateLogEntry(): Unit = this.synchronized {
    cancelScheduledFlush()
    scheduledFlush = scheduler.schedule(
      new Runnable {
        override def run(): Unit = flushAggregates()
      },
      debounceSeconds,
      TimeUnit.SECONDS
    )
  }
}
