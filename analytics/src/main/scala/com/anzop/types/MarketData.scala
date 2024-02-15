package com.anzop.types

import java.time.OffsetDateTime

final case class MarketDataMessage(messageType: MarketDataContent)

abstract class MarketDataContent {
  val symbol: String
  def marketTimestamp: OffsetDateTime
  def ingestTimestamp: OffsetDateTime
}

final case class Money(amount: BigDecimal, currency: String)

trait TradeUnit {
  val price: Money
  val lotSize: Double
}

final case class CryptoTradeUnit(price: Money, lotSize: Double) extends TradeUnit

final case class StockTradeUnit(exchange: String, price: Money, lotSize: Double) extends TradeUnit

trait Quotation {
  def bid: TradeUnit
  def ask: TradeUnit
}

trait Trade {
  def tradeId: Long
  def settle: TradeUnit
}

final case class CryptoQuotation(
    symbol: String,
    bid: CryptoTradeUnit,
    ask: CryptoTradeUnit,
    marketTimestamp: OffsetDateTime,
    ingestTimestamp: OffsetDateTime
  ) extends MarketDataContent
    with Quotation

final case class StockQuotation(
    symbol: String,
    bid: StockTradeUnit,
    ask: StockTradeUnit,
    marketTimestamp: OffsetDateTime,
    ingestTimestamp: OffsetDateTime,
    conditions: List[String],
    tape: String
  ) extends MarketDataContent
    with Quotation

final case class CryptoTrade(
    symbol: String,
    tradeId: Long,
    settle: CryptoTradeUnit,
    marketTimestamp: OffsetDateTime,
    ingestTimestamp: OffsetDateTime,
    tks: String
  ) extends MarketDataContent
    with Trade

final case class StockTrade(
    symbol: String,
    tradeId: Long,
    settle: StockTradeUnit,
    conditions: List[String],
    marketTimestamp: OffsetDateTime,
    ingestTimestamp: OffsetDateTime,
    tape: String
  ) extends MarketDataContent
    with Trade
