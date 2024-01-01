package types

final case class Money(amount: BigDecimal, currency: String)

trait MarketDataContent

final case class AlpacaStockQuotation(
    messageType: String,
    symbol: String,
    bidExchange: String,
    bidPrice: Money,
    bidSize: Double,
    askExchange: String,
    askPrice: Money,
    askSize: Double,
    marketTimestamp: java.time.OffsetDateTime,
    conditions: List[String],
    tape: String
  ) extends MarketDataContent

final case class AlpacaStockTrade(
    messageType: String,
    symbol: String,
    tradeId: Long,
    exchange: String,
    price: Money,
    lotSize: Double,
    tradeConditions: List[String],
    marketTimestamp: java.time.OffsetDateTime,
    tape: String
  ) extends MarketDataContent

final case class AlpacaCryptoQuotation(
    messageType: String,
    symbol: String,
    bidPrice: Money,
    bidSize: Double,
    askPrice: Money,
    askSize: Double,
    marketTimestamp: java.time.OffsetDateTime
  ) extends MarketDataContent

final case class AlpacaCryptoTrade(
    messageType: String,
    symbol: String,
    tradeId: Long,
    price: Money,
    size: Double,
    marketTimestamp: java.time.OffsetDateTime,
    tks: String
  ) extends MarketDataContent

final case class AlpacaMarketDataMessage(messageType: MarketDataContent)
