package net.anzop.config

import net.anzop.helpers.Extensions.EnvOps
import net.anzop.types.{MarketDataDeserializer, MarketDataMessage}
import org.apache.flink.configuration._
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer

import java.util.Properties

object StreamConfig {
  private def configureExecutionEnvironment(env: StreamExecutionEnvironment): Unit = {
    val checkpointPath = sys.env.getOrThrow("CHECKPOINT_PATH", "CHECKPOINT_PATH is not set")
    val cfg            = new Configuration()

    cfg.set(StateBackendOptions.STATE_BACKEND, "rocksdb")
    cfg.set(CheckpointingOptions.CHECKPOINT_STORAGE, "filesystem")
    cfg.set(CheckpointingOptions.CHECKPOINTS_DIRECTORY, checkpointPath)
    env.configure(cfg)
    env.getCheckpointConfig.setCheckpointInterval(60 * 1000L)
    env.setParallelism(1)
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
