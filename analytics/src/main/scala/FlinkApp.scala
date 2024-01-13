import appconfig.{InfluxDetails, KinesisProps, StreamConfig}
import helpers.StreamHelpers
import org.apache.flink.connector.kinesis.sink.KinesisStreamsSink
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows
import org.apache.flink.streaming.api.windowing.time.Time
import processors._
import results.WindowedQuotationVolumes
import sinks.{KinesisSink, ResultSink}
import types._

import java.time.{Duration => JavaDuration}
import java.util.Properties

object FlinkApp {

  private def flinkStream(influxDetails: InfluxDetails, kinesisProps: Properties): Unit = {
    val env = StreamConfig.createExecutionEnvironment()

    val marketDataStream: DataStream[MarketDataMessage] =
      env.addSource(StreamConfig.buildConsumer())

    val stockQuotationStream: DataStream[StockQuotation]   = StreamHelpers.filterType[StockQuotation](marketDataStream)
    val cryptoQuotationStream: DataStream[CryptoQuotation] = StreamHelpers.filterType[CryptoQuotation](marketDataStream)

    val watermarkedStockQuotationStream: DataStream[StockQuotation] = StreamHelpers.watermarkForBound(
      stockQuotationStream,
      dueTime      = JavaDuration.ofSeconds(180),
      idlePatience = JavaDuration.ofSeconds(30)
    )

    val watermarkedCryptoQuotationStream: DataStream[CryptoQuotation] = StreamHelpers.watermarkForBound(
      cryptoQuotationStream,
      dueTime      = JavaDuration.ofSeconds(180),
      idlePatience = JavaDuration.ofSeconds(30)
    )

    val windowedStockQuotationVolumes: DataStream[WindowedQuotationVolumes] = watermarkedStockQuotationStream
      .keyBy(_.symbol)
      .window(SlidingProcessingTimeWindows.of(Time.seconds(60), Time.seconds(30)))
      .apply(QuotationWindow.forStockQuotation())

    val windowedCryptoQuotationVolumes: DataStream[WindowedQuotationVolumes] = watermarkedCryptoQuotationStream
      .keyBy(_.symbol)
      .window(SlidingProcessingTimeWindows.of(Time.seconds(60), Time.seconds(30)))
      .apply(QuotationWindow.forCryptoQuotation())

    val influxDBSerializer = new WindowedQuotationVolumes.InfluxDBSerializer()
    windowedStockQuotationVolumes.addSink(ResultSink.forStockQuotation(influxDetails, influxDBSerializer))
    windowedCryptoQuotationVolumes.addSink(ResultSink.forCryptoQuotation(influxDetails, influxDBSerializer))

    val kinesisSink: KinesisStreamsSink[WindowedQuotationVolumes] =
      KinesisSink.make(kinesisProps, new WindowedQuotationVolumes.JsonSerializerSchema())

    windowedCryptoQuotationVolumes.sinkTo(kinesisSink)

    env.execute("Flink Kinesis Example")
  }

  def main(args: Array[String]): Unit = {
    StreamHelpers.checkInfluxDB()
    flinkStream(InfluxDetails.make(), KinesisProps.make())
  }
}
