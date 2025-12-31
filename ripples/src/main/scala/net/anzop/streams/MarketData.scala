package net.anzop.streams

import net.anzop.Ripples.logger
import net.anzop.config.{InfluxDetails, StreamConfig, WindowConfig}
import net.anzop.helpers.StreamHelpers
import net.anzop.helpers.StreamHelpers.nyseOpen
import net.anzop.processors.{QuotationDeltaProcessor, QuotationWindowProcessor, TradeDeltaProcessor, TradeWindowProcessor}
import net.anzop.results.WindowedTrades._
import net.anzop.results.{QuotationDeltas, TradeDeltas, WindowedQuotations, WindowedTrades}
import net.anzop.sinks.ResultSink
import net.anzop.types._
import org.apache.flink.streaming.api.scala._
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows
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

    val earliestTs = nyseOpen()

    val marketDataStream: DataStream[MarketDataMessage] =
      env
        .addSource(kinesisConsumer)
        .uid("kinesis-source")
        .filter(_.messageType.marketTimestamp.toInstant.toEpochMilli >= earliestTs)
        .name("redo-orders-of-today")

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

    val windowedStockQuotes: DataStream[WindowedQuotations] = watermarkedStockQuotesStream
      .keyBy[String]((x: StockQuotation) => x.symbol)
      .window(TumblingEventTimeWindows.of(windowConfig.windowPeriodLength))
      .apply(new QuotationWindowProcessor[StockQuotation])

    val windowedCryptoQuotes: DataStream[WindowedQuotations] = watermarkedCryptoQuotesStream
      .keyBy[String]((x: CryptoQuotation) => x.symbol)
      .window(TumblingEventTimeWindows.of(windowConfig.windowPeriodLength))
      .apply(new QuotationWindowProcessor[CryptoQuotation])

    val windowedStockTrades: DataStream[WindowedTrades] = watermarkedStockTradesStream
      .keyBy[String]((x: StockTrade) => x.symbol)
      .window(TumblingEventTimeWindows.of(windowConfig.windowPeriodLength))
      .apply(new TradeWindowProcessor[StockTrade])

    val windowedCryptoTrades: DataStream[WindowedTrades] = watermarkedCryptoTradesStream
      .keyBy[String]((x: CryptoTrade) => x.symbol)
      .window(TumblingEventTimeWindows.of(windowConfig.windowPeriodLength))
      .apply(new TradeWindowProcessor[CryptoTrade])

    logger.info("Flink stream windowing applied")

    val allWindowedQuotations: DataStream[WindowedQuotations] = windowedStockQuotes.union(windowedCryptoQuotes)
    val allWindowedTrades: DataStream[WindowedTrades]         = windowedStockTrades.union(windowedCryptoTrades)

    val quoteDifferences: DataStream[QuotationDeltas] = allWindowedQuotations
      .keyBy[String]((x: WindowedQuotations) => x.ticker)
      .process(new QuotationDeltaProcessor())

    val tradeDifferences: DataStream[TradeDeltas] = allWindowedTrades
      .keyBy[String]((x: WindowedTrades) => x.ticker)
      .process(new TradeDeltaProcessor())

    allWindowedQuotations.addSink(new ResultSink[WindowedQuotations](influxDetails))
    allWindowedTrades.addSink(new ResultSink[WindowedTrades](influxDetails))
    quoteDifferences.addSink(new ResultSink[QuotationDeltas](influxDetails))
    tradeDifferences.addSink(new ResultSink[TradeDeltas](influxDetails))
    logger.info("Flink stream results InfluxDB sinks created and connected")

    /*
    allWindowedQuotations.addSink(loggingKinesisSink[WindowedQuotes])
    allWindowedQuotations.sinkTo(KinesisSink.make[WindowedQuotes](kinesisProps))
    allWindowedTrades.addSink(loggingKinesisSink[WindowedTrades])
    allWindowedTrades.sinkTo(KinesisSink.make[WindowedTrades](kinesisProps))
    quoteDifferences.addSink(loggingKinesisSink[QuotationDeltas])
    quoteDifferences.sinkTo(KinesisSink.make[QuotationDeltas](kinesisProps))
    tradeDifferences.addSink(loggingKinesisSink[TradeDeltas])
    tradeDifferences.sinkTo(KinesisSink.make[TradeDeltas](kinesisProps))
    logger.info("Flink stream results Kinesis sinks created and connected")
     */

    env.execute("Flink Kinesis Example")
  }
}
