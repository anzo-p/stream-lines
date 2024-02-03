package com.anzop.sinks

import org.apache.flink.api.common.serialization.SerializationSchema
import org.apache.flink.connector.kinesis.sink.{KinesisStreamsSink, PartitionKeyGenerator}

import java.util.Properties

class SerializablePartitionKeyGenerator[T <: Serializable] extends PartitionKeyGenerator[T] with Serializable {
  override def apply(t: T): String = t.hashCode().toString
}

object KinesisSink {

  def make[T <: Serializable](producerConfig: Properties, serializationSchema: SerializationSchema[T]): KinesisStreamsSink[T] =
    KinesisStreamsSink
      .builder[T]()
      .setKinesisClientProperties(producerConfig)
      .setStreamName(producerConfig.getProperty("streamName"))
      .setSerializationSchema(serializationSchema)
      .setPartitionKeyGenerator(new SerializablePartitionKeyGenerator[T]())
      .setFailOnError(true)
      .build()
}
