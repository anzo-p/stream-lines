package net.anzop.config

import net.anzop.helpers.Extensions.EnvOps
import org.apache.flink.configuration.{Configuration, MemorySize, TaskManagerOptions}
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend
import org.apache.flink.runtime.state.storage.FileSystemCheckpointStorage
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

case class StreamConfig(pollInterval: Long)

object StreamConfig {
  private def configureExecutionEnvironment(env: StreamExecutionEnvironment): Unit = {
    val checkpointPath = sys.env.getOrThrow("CHECKPOINT_PATH", "CHECKPOINT_PATH is not set")
    val rocksDbBackend = new EmbeddedRocksDBStateBackend()
    env.setStateBackend(rocksDbBackend)
    env.getCheckpointConfig.setCheckpointStorage(new FileSystemCheckpointStorage(checkpointPath))
    env.enableCheckpointing(60 * 1000L)
    env.setParallelism(1)
  }

  def createExecutionEnvironment(): StreamExecutionEnvironment = {
    val config = new Configuration()
    config.set(TaskManagerOptions.MANAGED_MEMORY_SIZE, MemorySize.parse("256mb"))
    val env = StreamExecutionEnvironment.createLocalEnvironmentWithWebUI(config)
    StreamConfig.configureExecutionEnvironment(env)
    env
  }
}
