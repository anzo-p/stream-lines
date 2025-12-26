package net.anzop.sinks

import org.apache.flink.api.common.serialization.SerializationSchema
import org.apache.flink.connector.kinesis.sink.{KinesisStreamsSink, PartitionKeyGenerator}
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.slf4j.{Logger, LoggerFactory}

import java.util.Properties

class SerializablePartitionKeyGenerator[T <: Serializable] extends PartitionKeyGenerator[T] with Serializable {
  override def apply(t: T): String = t.hashCode().toString
}

object KinesisSink {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  def make[T <: Serializable](producerConfig: Properties)(implicit serializationSchema: SerializationSchema[T]): KinesisStreamsSink[T] =
    KinesisStreamsSink
      .builder[T]()
      .setKinesisClientProperties(producerConfig)
      .setStreamName(producerConfig.getProperty("streamName"))
      .setSerializationSchema(serializationSchema)
      .setPartitionKeyGenerator(new SerializablePartitionKeyGenerator[T]())
      .setFailOnError(true)
      .build()

  def loggingKinesisSink[T]: SinkFunction[T] = new SinkFunction[T] {
    override def invoke(value: T, context: SinkFunction.Context): Unit = {
      logger.info(s"Successfully storing ${value.getClass.getSimpleName} results to Kinesis")
    }
  }
}
