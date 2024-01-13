package sinks

import org.apache.flink.api.common.serialization.SerializationSchema
import org.apache.flink.connector.kinesis.sink.{KinesisStreamsSink, PartitionKeyGenerator}

import java.util.Properties

class SerializablePartitionKeyGenerator[T <: Serializable] extends PartitionKeyGenerator[T] with Serializable {
  override def apply(t: T): String = t.hashCode().toString
}

class KinesisSink[T <: Serializable](
    val producerConfig: Properties,
    val streamName: String,
    val serializationSchema: SerializationSchema[T]
  ) {

  private val partitionKeyGenerator = new SerializablePartitionKeyGenerator[T]()

  def createKinesisSink(): KinesisStreamsSink[T] =
    KinesisStreamsSink
      .builder[T]()
      .setKinesisClientProperties(producerConfig)
      .setStreamName(streamName)
      .setSerializationSchema(serializationSchema)
      .setPartitionKeyGenerator(partitionKeyGenerator)
      .setFailOnError(true)
      .build()
}

object KinesisSink {

  def make[T <: Serializable](producerConfig: Properties, serializationSchema: SerializationSchema[T]): KinesisStreamsSink[T] =
    new KinesisSink[T](producerConfig, producerConfig.getProperty("streamName"), serializationSchema)
      .createKinesisSink()

}
