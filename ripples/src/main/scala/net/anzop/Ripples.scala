package net.anzop

import com.amazonaws.services.kinesis.model.{AccessDeniedException, LimitExceededException, ResourceNotFoundException}
import net.anzop.sinks.KinesisSink.loggingKinesisSink
import net.anzop.config.{InfluxDetails, KinesisProps, StreamConfig, WindowConfig}
import net.anzop.helpers.StreamHelpers
import net.anzop.processors.QuotationWindow
import net.anzop.results.WindowedQuotationVolumes
import net.anzop.sinks.{KinesisSink, ResultSink}
import net.anzop.types.{CryptoQuotation, MarketDataMessage, StockQuotation}
import org.apache.flink.connector.kinesis.sink.KinesisStreamsSink
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.SlidingProcessingTimeWindows
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer
import org.slf4j.{Logger, LoggerFactory}

import java.util.Properties

object Ripples {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  private def flinkStream(influxDetails: InfluxDetails, kinesisProps: Properties): Unit = {
    logger.info("Starting Flink stream")

    val env = StreamConfig.createExecutionEnvironment()
    logger.info("Flink stream environment created")

    val windowConfig = WindowConfig.slidingWindows5m
    logger.info("Flink stream window configuration loaded")

    val kinesisConsumer: FlinkKinesisConsumer[MarketDataMessage] = StreamConfig.buildConsumer()
    logger.info("Flink Kinesis consumer created")

    val marketDataStream: DataStream[MarketDataMessage] =
      env.addSource(kinesisConsumer)

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

    logger.info("Flink stream watermarking applied")

    val windowedStockQuotationVolumes: DataStream[WindowedQuotationVolumes] = watermarkedStockQuotationStream
      .keyBy(_.symbol)
      .window(SlidingProcessingTimeWindows.of(windowConfig.windowPeriodLength, windowConfig.windowInterval))
      .apply(QuotationWindow.forStockQuotation())

    val windowedCryptoQuotationVolumes: DataStream[WindowedQuotationVolumes] = watermarkedCryptoQuotationStream
      .keyBy(_.symbol)
      .window(SlidingProcessingTimeWindows.of(windowConfig.windowPeriodLength, windowConfig.windowInterval))
      .apply(QuotationWindow.forCryptoQuotation())

    logger.info("Flink stream windowing applied")

    val influxDBSerializer = new WindowedQuotationVolumes.InfluxDBSerializer()
    windowedStockQuotationVolumes.addSink(new ResultSink(influxDetails, influxDBSerializer))
    windowedCryptoQuotationVolumes.addSink(new ResultSink(influxDetails, influxDBSerializer))
    logger.info("Flink stream results InfluxDB sink created")

    val kinesisSink: KinesisStreamsSink[WindowedQuotationVolumes] =
      KinesisSink.make(kinesisProps, new WindowedQuotationVolumes.JsonSerializerSchema())
    logger.info("Flink stream results Kinesis sink created")

    windowedStockQuotationVolumes.addSink(loggingKinesisSink[WindowedQuotationVolumes])
    windowedCryptoQuotationVolumes.addSink(loggingKinesisSink[WindowedQuotationVolumes])

    windowedStockQuotationVolumes.sinkTo(kinesisSink)
    windowedCryptoQuotationVolumes.sinkTo(kinesisSink)
    logger.info("Flink stream results sinks connected")

    env.execute("Flink Kinesis Example")
  }

  def main(args: Array[String]): Unit = {
    logger.info("Application starting")

    StreamHelpers.checkInfluxDB()
    val kinesisProps = KinesisProps.make()

    try {
      flinkStream(InfluxDetails.make(), kinesisProps)
    } catch {
      case e: ResourceNotFoundException =>
        logger.error(s"Kinesis stream ${kinesisProps.getOrDefault("streamName", "")} not found: ${e.getMessage}")

      case e: AccessDeniedException =>
        logger.error(s"Insufficient privileges for associated AWS Role to execute on Kinesis: ${e.getMessage}")

      case e: LimitExceededException =>
        logger.error(s"Attempt to exceed limits imposed by Kinesis: ${e.getMessage}")

      case e: Throwable =>
        logger.error(s"${e.getMessage}, ${e.getStackTrace.mkString("\n")}, ${e.getCause}}")
    }
  }
}
