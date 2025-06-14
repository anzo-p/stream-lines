package net.anzop.config

import net.anzop.helpers.Extensions.EnvOps
import org.apache.flink.configuration.{Configuration, MemorySize, TaskManagerOptions}
import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend
import org.apache.flink.runtime.state.storage.FileSystemCheckpointStorage
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment

object StreamConfig {
  private def configureExecutionEnvironment(env: StreamExecutionEnvironment): Unit = {
    val checkpointPath: String   = sys.env.getOrThrow("CHECKPOINT_PATH", "CHECKPOINT_PATH is not set")
    val checkpointInterval: Long = SourceRunnerConfig.values.interval
    val rocksDbBackend           = new EmbeddedRocksDBStateBackend()
    env.setStateBackend(rocksDbBackend)
    env.getCheckpointConfig.setCheckpointStorage(new FileSystemCheckpointStorage(checkpointPath))
    env.enableCheckpointing(checkpointInterval)
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
