package net.anzop.helpers

import net.anzop.types.{MarketDataContent, MarketDataMessage}
import org.apache.flink.api.common.eventtime.{SerializableTimestampAssigner, WatermarkStrategy}
import org.apache.flink.api.common.typeinfo.TypeInformation
import org.apache.flink.streaming.api.scala._
import org.slf4j.{Logger, LoggerFactory}

import java.time.Duration
import scala.reflect.ClassTag

object StreamHelpers {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  def filterType[T <: MarketDataContent : ClassTag : TypeInformation](stream: DataStream[MarketDataMessage]): DataStream[T] = {
    val clazz = implicitly[ClassTag[T]].runtimeClass
    stream
      .filter(_.messageType.getClass.isAssignableFrom(clazz))
      .map(_.messageType.asInstanceOf[T])(implicitly[TypeInformation[T]])
  }

  def watermarkForBound[T <: MarketDataContent](stream: DataStream[T], dueTime: Duration, idlePatience: Duration): DataStream[T] =
    stream
      .assignTimestampsAndWatermarks(
        WatermarkStrategy
          .forBoundedOutOfOrderness[T](dueTime)
          .withTimestampAssigner(
            new SerializableTimestampAssigner[T] {
              override def extractTimestamp(element: T, recordTimestamp: Long): Long =
                element.marketTimestamp.toInstant.toEpochMilli
            }
          )
          .withIdleness(idlePatience)
      )
}
