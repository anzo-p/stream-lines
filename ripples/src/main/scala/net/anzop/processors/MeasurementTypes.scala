package net.anzop.processors

import com.fasterxml.jackson.annotation.JsonValue

sealed trait WindowedMeasurement {

  @JsonValue
  def value: String
}

case object WindowedStockQuotesMeasurement extends WindowedMeasurement {
  override val value = "stock-quotation-aggregates-sliding-window"
}

case object WindowedStockTradesMeasurement extends WindowedMeasurement {
  override val value = "stock-trade-aggregates-sliding-window"
}

case object WindowedCryptoQuotesMeasurement extends WindowedMeasurement {
  override val value = "crypto-quotation-aggregates-sliding-window"
}

case object WindowedCryptoTradesMeasurement extends WindowedMeasurement {
  override val value = "crypto-trade-aggregates-sliding-window"
}
