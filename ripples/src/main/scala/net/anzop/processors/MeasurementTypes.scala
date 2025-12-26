package net.anzop.processors

import com.fasterxml.jackson.annotation.JsonValue

sealed trait InfluxMeasurement {

  @JsonValue
  def value: String
}

sealed trait WindowedMeasurement

case object WindowedQuotesMeasurement extends WindowedMeasurement with InfluxMeasurement {
  override val value = "quotation-aggregates-tumbling-window"
}

case object WindowedTradesMeasurement extends WindowedMeasurement with InfluxMeasurement {
  override val value = "trade-aggregates-tumbling-window"
}

sealed trait DeltaMeasurement

case object QuotesDeltaMeasurement extends DeltaMeasurement with InfluxMeasurement {
  override val value = "quotation-deltas"
}

case object TradesDeltaMeasurement extends DeltaMeasurement with InfluxMeasurement {
  override val value = "trade-deltas"
}
