package com.anzop.types

import com.anzop.helpers.Monetary
import com.anzop.types.TimeExtensions._
import market_data.crypto_quotation.CryptoQuotationProto
import market_data.crypto_trade.CryptoTradeProto
import market_data.market_data.MarketDataProto
import market_data.money.MoneyProto
import market_data.stock_quotation.StockQuotationProto
import market_data.stock_trade.StockTradeProto
import market_data.trade_unit.{CryptoTradeUnitProto, StockTradeUnitProto}

import java.time.OffsetDateTime

object ProtobufSerdes {

  private def extractTimestamp(ts: Option[com.google.protobuf.timestamp.Timestamp], fieldName: String): OffsetDateTime =
    ts.map(_.toJavaOffsetDateTime)
      .getOrElse(throw new IllegalArgumentException(s"$fieldName is required for message type"))

  private def fromProtobuf(proto: MoneyProto): Money =
    Monetary.toDecimal(proto.units, proto.nanos) match {
      case Right(value) => Money(value, proto.currency)
      case Left(error)  => throw new IllegalArgumentException(error)
    }

  private def fromProtobuf(proto: CryptoTradeUnitProto): CryptoTradeUnit =
    CryptoTradeUnit(
      price   = fromProtobuf(proto.price.getOrElse(throw new IllegalArgumentException("Price data is required"))),
      lotSize = proto.lotSize
    )

  private def fromProtobuf(proto: StockTradeUnitProto): StockTradeUnit =
    StockTradeUnit(
      exchange = proto.exchange,
      price    = fromProtobuf(proto.price.getOrElse(throw new IllegalArgumentException("Price data is required"))),
      lotSize  = proto.lotSize
    )

  private def fromProtobuf(proto: CryptoQuotationProto, marketTimestamp: OffsetDateTime, ingestTimestamp: OffsetDateTime): CryptoQuotation =
    CryptoQuotation(
      symbol          = proto.symbol,
      bid             = fromProtobuf(proto.bid.getOrElse(throw new IllegalArgumentException("Bid data is required"))),
      ask             = fromProtobuf(proto.ask.getOrElse(throw new IllegalArgumentException("Ask data is required"))),
      marketTimestamp = marketTimestamp,
      ingestTimestamp = ingestTimestamp
    )

  private def fromProtobuf(proto: CryptoTradeProto, marketTimestamp: OffsetDateTime, ingestTimestamp: OffsetDateTime): CryptoTrade =
    CryptoTrade(
      symbol          = proto.symbol,
      tradeId         = proto.tradeId,
      settle          = fromProtobuf(proto.settle.getOrElse(throw new IllegalArgumentException("Settle data is required"))),
      marketTimestamp = marketTimestamp,
      ingestTimestamp = ingestTimestamp,
      tks             = proto.tks
    )

  private def fromProtobuf(proto: StockQuotationProto, marketTimestamp: OffsetDateTime, ingestTimestamp: OffsetDateTime): StockQuotation =
    StockQuotation(
      symbol          = proto.symbol,
      bid             = fromProtobuf(proto.bid.getOrElse(throw new IllegalArgumentException("Bid data is required"))),
      ask             = fromProtobuf(proto.ask.getOrElse(throw new IllegalArgumentException("Ask data is required"))),
      marketTimestamp = marketTimestamp,
      ingestTimestamp = ingestTimestamp,
      conditions      = proto.conditions.toList,
      tape            = proto.tape
    )

  private def fromProtoBuf(proto: StockTradeProto, marketTimestamp: OffsetDateTime, ingestTimestamp: OffsetDateTime): StockTrade =
    StockTrade(
      symbol          = proto.symbol,
      tradeId         = proto.tradeId,
      settle          = fromProtobuf(proto.settle.getOrElse(throw new IllegalArgumentException("settle data is required"))),
      marketTimestamp = marketTimestamp,
      ingestTimestamp = ingestTimestamp,
      conditions      = proto.conditions.toList,
      tape            = proto.tape
    )

  def fromProtoBuf(proto: MarketDataProto): MarketDataMessage = {
    val marketTimestamp = extractTimestamp(proto.marketTimestamp, "marketTimestamp")
    val ingestTimestamp = extractTimestamp(proto.ingestTimestamp, "ingestTimestamp")

    MarketDataMessage(
      messageType = proto.messageType match {
        case MarketDataProto.MessageType.Cqm(message: CryptoQuotationProto) =>
          fromProtobuf(message, marketTimestamp, ingestTimestamp)

        case MarketDataProto.MessageType.Ctm(message: CryptoTradeProto) =>
          fromProtobuf(message, marketTimestamp, ingestTimestamp)

        case MarketDataProto.MessageType.Sqm(message: StockQuotationProto) =>
          fromProtobuf(message, marketTimestamp, ingestTimestamp)

        case MarketDataProto.MessageType.Stm(message: StockTradeProto) =>
          fromProtoBuf(message, marketTimestamp, ingestTimestamp)

        case _ => throw new IllegalArgumentException("Cannot process messageType")
      }
    )
  }
}
