import market_data.market_data.MarketDataMessageProto
import org.apache.flink.api.common.typeinfo.TypeInformation
// import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.scala.{createTypeInformation, DataStream, StreamExecutionEnvironment}
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer
import org.apache.flink.streaming.connectors.kinesis.serialization.KinesisDeserializationSchema
import types.{AlpacaMarketDataMessage, AlpacaSerDes}

import java.util.Properties

class MarketDataDeserializer extends KinesisDeserializationSchema[AlpacaMarketDataMessage] {
  override def deserialize(
      data: Array[Byte],
      partitionKey: String,
      seqNum: String,
      approxArrivalTimestamp: Long,
      stream: String,
      shardId: String
    ): AlpacaMarketDataMessage = {
    AlpacaSerDes.fromProtoBuf(MarketDataMessageProto.parseFrom(data))
  }

  override def getProducedType: TypeInformation[AlpacaMarketDataMessage] = {
    TypeInformation.of(classOf[AlpacaMarketDataMessage])
  }
}

object FlinkApp {

  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment

    val properties = new Properties()
    properties.setProperty("aws.region", "eu-west-1")
    properties.setProperty("flink.stream.initpos", "TRIM_HORIZON")

    val kinesisConsumer = new FlinkKinesisConsumer[AlpacaMarketDataMessage](
      "control-tower-downstream",
      new MarketDataDeserializer(),
      properties
    )

    val stream: DataStream[AlpacaMarketDataMessage] = env.addSource(kinesisConsumer)

    stream.print()

    env.execute("Flink Kinesis Example")
  }
}
