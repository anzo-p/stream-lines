package net.anzop.config

import net.anzop.helpers.Extensions.EnvOps
import net.anzop.types.{MarketDataDeserializer, MarketDataMessage}
import org.apache.flink.configuration.{Configuration, MemorySize, TaskManagerOptions}
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend
import org.apache.flink.runtime.state.storage.FileSystemCheckpointStorage
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer

import java.util.Properties

object StreamConfig {
  private def configureExecutionEnvironment(env: StreamExecutionEnvironment): Unit = {
    val checkpointPath = sys.env.getOrThrow("CHECKPOINT_PATH", "CHECKPOINT_PATH is not set")
    val rocksDbBackend = new EmbeddedRocksDBStateBackend()
    env.setStateBackend(rocksDbBackend)
    env.getCheckpointConfig.setCheckpointStorage(new FileSystemCheckpointStorage(checkpointPath))
    env.enableCheckpointing(60 * 1000L)
    env.setParallelism(2)
  }

  def createExecutionEnvironment(): StreamExecutionEnvironment = {
    val config = new Configuration()
    config.set(TaskManagerOptions.MANAGED_MEMORY_SIZE, MemorySize.parse("256mb"))
    val env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config)
    StreamConfig.configureExecutionEnvironment(env)
    env
  }

  def buildConsumer(): FlinkKinesisConsumer[MarketDataMessage] = {
    val consumerConfig = new Properties()
    consumerConfig.setProperty("aws.region", sys.env.getOrThrow("AWS_REGION", "AWS_REGION is not set"))
    consumerConfig.setProperty("flink.stream.initpos", "LATEST")

    new FlinkKinesisConsumer[MarketDataMessage](
      sys.env.getOrThrow("KINESIS_UPSTREAM_NAME", "KINESIS_UPSTREAM_NAME is not set"),
      new MarketDataDeserializer(),
      consumerConfig
    )
  }
}
