package processors

import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala.DataStream
import types.{MarketDataContent, MarketDataMessage}

import java.time.Duration

object StreamHelpers {

  def filterAndMap[T <: MarketDataContent : TypeInformation](stream: DataStream[MarketDataMessage]): DataStream[T] = {
    stream
      .filter(_.messageType.isInstanceOf[T])
      .map(_.messageType.asInstanceOf[T])(implicitly[TypeInformation[T]])
  }

  def watermarkForBound[T <: MarketDataContent](stream: DataStream[T], patience: Duration, slack: Duration): DataStream[T] =
    stream
      .assignTimestampsAndWatermarks(
        WatermarkStrategy
          .forBoundedOutOfOrderness[T](patience)
          .withTimestampAssigner(
            new SerializableTimestampAssigner[T] {
              override def extractTimestamp(element: T, recordTimestamp: Long): Long =
                element.marketTimestamp.toInstant.toEpochMilli
            }
          )
          .withIdleness(slack)
      )
}
