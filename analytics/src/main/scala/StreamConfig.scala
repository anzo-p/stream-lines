import org.apache.flink.contrib.streaming.state.EmbeddedRocksDBStateBackend
import org.apache.flink.runtime.state.storage.FileSystemCheckpointStorage
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer
import types.{MarketDataDeserializer, MarketDataMessage}

import java.util.Properties

object StreamConfig {

  def buildConsumer(): FlinkKinesisConsumer[MarketDataMessage] = {
    val consumerConfig = new Properties()
    consumerConfig.setProperty("aws.region", System.getenv().getOrDefault("AWS_REGION", ""))
    consumerConfig.setProperty("flink.stream.initpos", "LATEST")

    new FlinkKinesisConsumer[MarketDataMessage](
      System.getenv().getOrDefault("KINESIS_STREAM_NAME", ""),
      new MarketDataDeserializer(),
      consumerConfig
    )
  }

  def configExecEnv(env: StreamExecutionEnvironment): Unit = {
    val checkpointPath = sys.env.getOrElse("CHECKPOINT_PATH", "")
    val rocksDbBackend = new EmbeddedRocksDBStateBackend()
    env.setStateBackend(rocksDbBackend)
    env.getCheckpointConfig.setCheckpointStorage(new FileSystemCheckpointStorage(checkpointPath))
    env.enableCheckpointing(1000)
  }
}
