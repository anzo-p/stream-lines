package net.anzop.sinks

import net.anzop.results.WindowedQuotationVolumes
import org.apache.flink.api.common.serialization.SerializationSchema
import org.apache.flink.connector.kinesis.sink.{KinesisStreamsSink, PartitionKeyGenerator}
import org.apache.flink.streaming.api.functions.sink.SinkFunction
import org.slf4j.LoggerFactory

import java.util.Properties

class WindowedVolumesPartitionKeyGenerator extends PartitionKeyGenerator[WindowedQuotationVolumes] with Serializable {

  override def apply(v: WindowedQuotationVolumes): String =
    v.symbol
}

object KinesisSink {
  private val logger = LoggerFactory.getLogger(getClass)

  def make(producerConfig: Properties): KinesisStreamsSink[WindowedQuotationVolumes] = {
    val schema: SerializationSchema[WindowedQuotationVolumes] =
      new WindowedQuotationVolumes.JsonSerializerSchema()

    logger.info(s"Creating Kinesis sink with schema: ${schema.getClass.getName}")

    KinesisStreamsSink
      .builder[WindowedQuotationVolumes]()
      .setKinesisClientProperties(producerConfig)
      .setStreamName(producerConfig.getProperty("streamName"))
      .setSerializationSchema(schema)
      .setPartitionKeyGenerator(new WindowedVolumesPartitionKeyGenerator)
      .setFailOnError(true)
      .build()
  }

  def loggingKinesisSink[WindowedQuotationVolumes]: SinkFunction[WindowedQuotationVolumes] =
    new SinkFunction[WindowedQuotationVolumes] {
      override def invoke(value: WindowedQuotationVolumes, context: SinkFunction.Context): Unit = {
        logger.info(s"Successfully storing ${value.getClass.getSimpleName} results to Kinesis")
      }
    }
}
