package types

import market_data.market_data.MarketDataProto
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.connectors.kinesis.serialization.KinesisDeserializationSchema

class MarketDataDeserializer extends KinesisDeserializationSchema[MarketDataMessage] {
  override def deserialize(
      data: Array[Byte],
      partitionKey: String,
      seqNum: String,
      approxArrivalTimestamp: Long,
      stream: String,
      shardId: String
    ): MarketDataMessage = {
    ProtobufSerdes.fromProtoBuf(MarketDataProto.parseFrom(data))
  }

  override def getProducedType: TypeInformation[MarketDataMessage] = {
    TypeInformation.of(classOf[MarketDataMessage])
  }
}
