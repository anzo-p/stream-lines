package net.anzop.sinks

import org.apache.flink.api.common.serialization.SerializationSchema
import org.apache.flink.connector.kinesis.sink.{KinesisStreamsSink, PartitionKeyGenerator}
import org.slf4j.{Logger, LoggerFactory}

import java.util.Properties

class LoggingSerializationSchema[T <: Serializable](delegate: SerializationSchema[T]) extends SerializationSchema[T] {

  private val logger = LoggerFactory.getLogger(getClass)

  override def serialize(element: T): Array[Byte] = {
    val bytes = delegate.serialize(element)
    logger.info(
      s"Kinesis produce: partitionKey=${element.hashCode()} " +
        s"payload=${new String(bytes, java.nio.charset.StandardCharsets.UTF_8)}"
    )
    bytes
  }
}

class SerializablePartitionKeyGenerator[T <: Serializable] extends PartitionKeyGenerator[T] with Serializable {
  override def apply(t: T): String = t.hashCode().toString
}

object KinesisSink {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  def make[T <: Serializable](producerConfig: Properties)(implicit serializationSchema: SerializationSchema[T]): KinesisStreamsSink[T] = {

    val loggingSchema = new LoggingSerializationSchema(serializationSchema)

    KinesisStreamsSink
      .builder[T]()
      .setKinesisClientProperties(producerConfig)
      .setStreamName(producerConfig.getProperty("streamName"))
      .setSerializationSchema(loggingSchema)
      .setPartitionKeyGenerator(new SerializablePartitionKeyGenerator[T]())
      .setFailOnError(false)
      .build()
  }
}
