package com.anzop

import com.anzop.appconfig.{InfluxDetails, KinesisProps, StreamConfig, WindowConfig}
import com.anzop.helpers.StreamHelpers
import com.anzop.processors.QuotationWindow
import com.anzop.results.WindowedQuotationVolumes
import com.anzop.sinks.{KinesisSink, ResultSink}
import com.anzop.types.{CryptoQuotation, MarketDataMessage, StockQuotation}
import org.apache.flink.connector.kinesis.sink.KinesisStreamsSink
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows

import java.util.Properties

object FlinkApp {
  private def flinkStream(influxDetails: InfluxDetails, kinesisProps: Properties): Unit = {
    val env = StreamConfig.createExecutionEnvironment()

    val windowConfig = WindowConfig.slidingWindows5m

    val marketDataStream: DataStream[MarketDataMessage] =
      env.addSource(StreamConfig.buildConsumer())

    val stockQuotationStream: DataStream[StockQuotation]   = StreamHelpers.filterType[StockQuotation](marketDataStream)
    val cryptoQuotationStream: DataStream[CryptoQuotation] = StreamHelpers.filterType[CryptoQuotation](marketDataStream)

    val watermarkedStockQuotationStream: DataStream[StockQuotation] = StreamHelpers.watermarkForBound(
      stockQuotationStream,
      dueTime      = windowConfig.watermark.dueTime,
      idlePatience = windowConfig.watermark.idlePatience
    )

    val watermarkedCryptoQuotationStream: DataStream[CryptoQuotation] = StreamHelpers.watermarkForBound(
      cryptoQuotationStream,
      dueTime      = windowConfig.watermark.dueTime,
      idlePatience = windowConfig.watermark.idlePatience
    )

    val windowedStockQuotationVolumes: DataStream[WindowedQuotationVolumes] = watermarkedStockQuotationStream
      .keyBy(_.symbol)
      .window(SlidingProcessingTimeWindows.of(windowConfig.windowPeriodLength, windowConfig.windowInterval))
      .apply(QuotationWindow.forStockQuotation())

    val windowedCryptoQuotationVolumes: DataStream[WindowedQuotationVolumes] = watermarkedCryptoQuotationStream
      .keyBy(_.symbol)
      .window(SlidingProcessingTimeWindows.of(windowConfig.windowPeriodLength, windowConfig.windowInterval))
      .apply(QuotationWindow.forCryptoQuotation())

    val influxDBSerializer = new WindowedQuotationVolumes.InfluxDBSerializer()
    windowedStockQuotationVolumes.addSink(new ResultSink(influxDetails, influxDBSerializer))
    windowedCryptoQuotationVolumes.addSink(new ResultSink(influxDetails, influxDBSerializer))

    val kinesisSink: KinesisStreamsSink[WindowedQuotationVolumes] =
      KinesisSink.make(kinesisProps, new WindowedQuotationVolumes.JsonSerializerSchema())

    windowedStockQuotationVolumes.sinkTo(kinesisSink)
    windowedCryptoQuotationVolumes.sinkTo(kinesisSink)

    env.execute("Flink Kinesis Example")
  }

  def main(args: Array[String]): Unit = {
    StreamHelpers.checkInfluxDB()
    flinkStream(InfluxDetails.make(), KinesisProps.make())
  }
}
