package net.anzop.sinks

import net.anzop.results.WindowedQuotationVolumes
import net.anzop.results.WindowedQuotationVolumes.JsonSerializerSchema
import org.apache.flink.connector.kinesis.sink.{KinesisStreamsSink, PartitionKeyGenerator}
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.slf4j.{Logger, LoggerFactory}

import java.util.Properties

class SerializablePartitionKeyGenerator[T <: Serializable] extends PartitionKeyGenerator[T] with Serializable {
  override def apply(t: T): String = t.hashCode().toString
}

object KinesisSink {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  def make[T <: Serializable](producerConfig: Properties): KinesisStreamsSink[WindowedQuotationVolumes] =
    KinesisStreamsSink
      .builder[WindowedQuotationVolumes]()
      .setKinesisClientProperties(producerConfig)
      .setStreamName(producerConfig.getProperty("streamName"))
      .setSerializationSchema(new JsonSerializerSchema())
      .setPartitionKeyGenerator(new SerializablePartitionKeyGenerator[WindowedQuotationVolumes]())
      .setFailOnError(true)
      .build()

  def loggingKinesisSink[T]: SinkFunction[T] = new SinkFunction[T] {
    override def invoke(value: T, context: SinkFunction.Context): Unit = {
      logger.info(s"Successfully storing ${value.getClass.getSimpleName} results to Kinesis")
    }
  }
}
