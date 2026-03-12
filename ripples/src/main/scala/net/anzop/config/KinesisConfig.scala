package net.anzop.config

import net.anzop.helpers.MapExtensions.EnvOps
import net.anzop.types.{MarketDataDeserializer, MarketDataMessage}
import org.apache.flink.kinesis.shaded.org.apache.flink.connector.aws.config.AWSConfigConstants
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer
import org.apache.flink.streaming.connectors.kinesis.config.ConsumerConfigConstants

import java.util.Properties

object KinesisConfig {

  def buildConsumer(): FlinkKinesisConsumer[MarketDataMessage] = {
    val consumerConfig = new Properties()
    consumerConfig.setProperty("aws.region", sys.env.getOrThrow("AWS_REGION", "AWS_REGION is not set"))
    consumerConfig.setProperty(
      ConsumerConfigConstants.STREAM_INITIAL_POSITION,
      ConsumerConfigConstants.InitialPosition.LATEST.name()
    )

    new FlinkKinesisConsumer[MarketDataMessage](
      sys.env.getOrThrow("KINESIS_UPSTREAM_NAME", "KINESIS_UPSTREAM_NAME is not set"),
      new MarketDataDeserializer(),
      consumerConfig
    )
  }

  def makeProducerConfig(): Properties = {
    val props = new Properties()
    props.setProperty(AWSConfigConstants.AWS_REGION, sys.env.getOrThrow("AWS_REGION", "AWS_REGION is not set"))
    props.setProperty("streamName", sys.env.getOrThrow("KINESIS_DOWNSTREAM_NAME", "KINESIS_DOWNSTREAM_NAME is not set"))
    props
  }
}
