package net.anzop.sinks.influxdb

import net.anzop.config.InfluxConfig
import org.apache.flink.streaming.api.functions.sink.{RichSinkFunction, SinkFunction}
import org.slf4j.{Logger, LoggerFactory}

trait InfluxBaseSink[T <: InfluxSerializable] extends RichSinkFunction[T] {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  def influxDetails: InfluxConfig

  def send(serialized: String): Unit

  override def invoke(value: T, context: SinkFunction.Context): Unit =
    Option(value.toPoint)
      .filter(_.hasFields)
      .map(_.toLineProtocol)
      .fold(logger.warn("Empty data received, skipping"))(send)
}
