package net.anzop.streams

import net.anzop.Ripples.logger
import net.anzop.config.{InfluxDetails, StreamConfig, WindowConfig}
import net.anzop.helpers.StreamHelpers
import net.anzop.processors.{QuotationWindow, TradeWindow}
import net.anzop.results.{WindowedQuotes, WindowedTrades}
import net.anzop.sinks.ResultSink
import net.anzop.types.{CryptoQuotation, CryptoTrade, MarketDataMessage, StockQuotation, StockTrade}
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows
import org.apache.flink.streaming.connectors.kinesis.FlinkKinesisConsumer

import java.util.Properties

object MarketData {

  def stream(influxDetails: InfluxDetails, kinesisProps: Properties): Unit = {
    logger.info("Starting Flink stream")

    val env = StreamConfig.createExecutionEnvironment()
    logger.info("Flink stream environment created")

    val windowConfig = WindowConfig.slidingWindows5m
    logger.info("Flink stream window configuration loaded")

    val kinesisConsumer: FlinkKinesisConsumer[MarketDataMessage] = StreamConfig.buildConsumer()
    logger.info("Flink Kinesis consumer created")

    val marketDataStream: DataStream[MarketDataMessage] =
      env.addSource(kinesisConsumer)

    val stockQuotesStream: DataStream[StockQuotation]   = StreamHelpers.filterType[StockQuotation](marketDataStream)
    val cryptoQuotesStream: DataStream[CryptoQuotation] = StreamHelpers.filterType[CryptoQuotation](marketDataStream)
    val stockTradesStream: DataStream[StockTrade]       = StreamHelpers.filterType[StockTrade](marketDataStream)
    val cryptoTradesStream: DataStream[CryptoTrade]     = StreamHelpers.filterType[CryptoTrade](marketDataStream)

    val watermarkedStockQuotesStream: DataStream[StockQuotation] = StreamHelpers.watermarkForBound(
      stockQuotesStream,
      dueTime      = windowConfig.watermark.dueTime,
      idlePatience = windowConfig.watermark.idlePatience
    )
    val watermarkedCryptoQuotesStream: DataStream[CryptoQuotation] = StreamHelpers.watermarkForBound(
      cryptoQuotesStream,
      dueTime      = windowConfig.watermark.dueTime,
      idlePatience = windowConfig.watermark.idlePatience
    )
    val watermarkedStockTradesStream: DataStream[StockTrade] = StreamHelpers.watermarkForBound(
      stockTradesStream,
      dueTime      = windowConfig.watermark.dueTime,
      idlePatience = windowConfig.watermark.idlePatience
    )
    val watermarkedCryptoTradesStream: DataStream[CryptoTrade] = StreamHelpers.watermarkForBound(
      cryptoTradesStream,
      dueTime      = windowConfig.watermark.dueTime,
      idlePatience = windowConfig.watermark.idlePatience
    )
    logger.info("Flink stream watermarking applied")

    val windowedStockQuotes: DataStream[WindowedQuotes] = watermarkedStockQuotesStream
      .keyBy[String]((x: StockQuotation) => x.symbol)
      .window(SlidingEventTimeWindows.of(windowConfig.windowPeriodLength, windowConfig.windowInterval))
      .apply(QuotationWindow.forStockQuotation())

    val windowedCryptoQuotes: DataStream[WindowedQuotes] = watermarkedCryptoQuotesStream
      .keyBy[String]((x: CryptoQuotation) => x.symbol)
      .window(SlidingEventTimeWindows.of(windowConfig.windowPeriodLength, windowConfig.windowInterval))
      .apply(QuotationWindow.forCryptoQuotation())

    val windowedStockTrades: DataStream[WindowedTrades] = watermarkedStockTradesStream
      .keyBy[String]((x: StockTrade) => x.symbol)
      .window(SlidingEventTimeWindows.of(windowConfig.windowPeriodLength, windowConfig.windowInterval))
      .apply(TradeWindow.forStockTrade())

    val windowedCryptoTrades: DataStream[WindowedTrades] = watermarkedCryptoTradesStream
      .keyBy[String]((x: CryptoTrade) => x.symbol)
      .window(SlidingEventTimeWindows.of(windowConfig.windowPeriodLength, windowConfig.windowInterval))
      .apply(TradeWindow.forCryptoTrade())
    logger.info("Flink stream windowing applied")

    val windowedQuotesInfluxSink = new ResultSink(influxDetails, new WindowedQuotes.InfluxDBSerializer())
    windowedStockQuotes.addSink(windowedQuotesInfluxSink)
    windowedCryptoQuotes.addSink(windowedQuotesInfluxSink)

    val windowedTradesInfluxSink = new ResultSink(influxDetails, new WindowedTrades.InfluxDBSerializer())
    windowedStockTrades.addSink(windowedTradesInfluxSink)
    windowedCryptoTrades.addSink(windowedTradesInfluxSink)
    logger.info("Flink stream results InfluxDB sink created")

    /*
    val kinesisSink: KinesisStreamsSink[WindowedQuotationVolumes] = KinesisSink.make(kinesisProps)
    logger.info("Flink stream results Kinesis sink created")

    windowedStockQuotes.addSink(loggingKinesisSink[WindowedQuotationVolumes])
    windowedCryptoQuotes.addSink(loggingKinesisSink[WindowedQuotationVolumes])

    windowedStockQuotes.sinkTo(kinesisSink)
    windowedCryptoQuotes.sinkTo(kinesisSink)
    windowedStockTrades.sinkTo(kinesisSink)
    windowedCryptoTrades.sinkTo(kinesisSink)
    logger.info("Flink stream results sinks connected")
     */

    env.execute("Flink Kinesis Example")
  }
}
