package types

import market_data.crypto_quotation.CryptoQuotationMessageProto
import market_data.crypto_trade.CryptoTradeMessageProto
import market_data.market_data.MarketDataMessageProto
import market_data.money.MoneyMessageProto
import market_data.stock_quotation.StockQuotationMessageProto
import market_data.stock_trade.StockTradeMessageProto
import types.TimeExtensions._

object AlpacaSerDes {

  def fromProtoBuf(proto: MoneyMessageProto): Money =
    Money(
      amount   = proto.u + proto.n,
      currency = proto.c
    )

  def fromProtoBuf(proto: MarketDataMessageProto): AlpacaMarketDataMessage =
    AlpacaMarketDataMessage(
      messageType = proto.messageType match {
        case MarketDataMessageProto.MessageType.Cqm(m: CryptoQuotationMessageProto) =>
          fromProtobuf(m)
        case MarketDataMessageProto.MessageType.Ctm(m: CryptoTradeMessageProto) =>
          fromProtobuf(m)
        case MarketDataMessageProto.MessageType.Sqm(m: StockQuotationMessageProto) =>
          fromProtobuf(m)
        case MarketDataMessageProto.MessageType.Stm(m: StockTradeMessageProto) =>
          fromProtoBuf(m)
      }
    )

  def fromProtobuf(proto: StockQuotationMessageProto): AlpacaStockQuotation =
    AlpacaStockQuotation(
      messageType     = proto.ty,
      symbol          = proto.sy,
      bidExchange     = proto.bx,
      bidPrice        = fromProtoBuf(proto.bp.get),
      bidSize         = proto.bs,
      askExchange     = proto.ax,
      askPrice        = fromProtoBuf(proto.ap.get),
      askSize         = proto.as,
      marketTimestamp = proto.ti.map(_.toJavaOffsetDateTime).get,
      conditions      = proto.c.toList,
      tape            = proto.z
    )

  def fromProtoBuf(proto: StockTradeMessageProto): AlpacaStockTrade =
    AlpacaStockTrade(
      messageType     = proto.ty,
      symbol          = proto.sy,
      tradeId         = proto.i,
      exchange        = proto.x,
      price           = fromProtoBuf(proto.p.get),
      lotSize         = proto.s,
      tradeConditions = proto.c.toList,
      marketTimestamp = proto.ti.map(_.toJavaOffsetDateTime).get,
      tape            = proto.z
    )

  def fromProtobuf(proto: CryptoQuotationMessageProto): AlpacaCryptoQuotation =
    AlpacaCryptoQuotation(
      messageType     = proto.ty,
      symbol          = proto.sy,
      bidPrice        = fromProtoBuf(proto.bp.get),
      bidSize         = proto.bs,
      askPrice        = fromProtoBuf(proto.ap.get),
      askSize         = proto.as,
      marketTimestamp = proto.ti.map(_.toJavaOffsetDateTime).get
    )

  def fromProtobuf(proto: CryptoTradeMessageProto): AlpacaCryptoTrade =
    AlpacaCryptoTrade(
      messageType     = proto.ty,
      symbol          = proto.sy,
      tradeId         = proto.i,
      price           = fromProtoBuf(proto.p.get),
      size            = proto.s,
      marketTimestamp = proto.ti.map(_.toJavaOffsetDateTime).get,
      tks             = proto.tks
    )

  def toProtobuf(model: Money): MoneyMessageProto = {
    MoneyMessageProto(
      u = model.amount.toLong,
      n = (model.amount - model.amount.toLong).doubleValue(),
      c = model.currency
    )
  }

  def toProtobuf(model: AlpacaStockQuotation): StockQuotationMessageProto =
    StockQuotationMessageProto(
      ty = model.messageType,
      sy = model.symbol,
      bx = model.bidExchange,
      bp = Some(toProtobuf(model.bidPrice)),
      bs = model.bidSize,
      ax = model.askExchange,
      ap = Some(toProtobuf(model.askPrice)),
      as = model.askSize,
      ti = Some(model.marketTimestamp.toProtobufTs),
      c  = model.conditions,
      z  = model.tape
    )

  def toProtobuf(model: AlpacaStockTrade): StockTradeMessageProto =
    StockTradeMessageProto(
      ty = model.messageType,
      sy = model.symbol,
      i  = model.tradeId,
      x  = model.exchange,
      p  = Some(toProtobuf(model.price)),
      s  = model.lotSize,
      c  = model.tradeConditions,
      ti = Some(model.marketTimestamp.toProtobufTs),
      z  = model.tape
    )

  def toProtobuf(model: AlpacaCryptoQuotation): CryptoQuotationMessageProto =
    CryptoQuotationMessageProto(
      ty = model.messageType,
      sy = model.symbol,
      bp = Some(toProtobuf(model.bidPrice)),
      bs = model.bidSize,
      ap = Some(toProtobuf(model.askPrice)),
      as = model.askSize,
      ti = Some(model.marketTimestamp.toProtobufTs)
    )

  def toProtobuf(model: AlpacaCryptoTrade): CryptoTradeMessageProto =
    CryptoTradeMessageProto(
      ty  = model.messageType,
      sy  = model.symbol,
      i   = model.tradeId,
      p   = Some(toProtobuf(model.price)),
      s   = model.size,
      ti  = Some(model.marketTimestamp.toProtobufTs),
      tks = model.tks
    )
}
