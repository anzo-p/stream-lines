package net.anzop.processors

import com.fasterxml.jackson.annotation.JsonValue

sealed trait WindowedVolumesMeasurement {

  @JsonValue
  def value: String
}

case object WindowedStockQuotationVolumesMeasurement extends WindowedVolumesMeasurement {
  override val value = "stock-quotation-aggregates-sliding-window"
}

case object WindowedCryptoQuotationVolumesMeasurement extends WindowedVolumesMeasurement {
  override val value = "crypto-quotation-aggregates-sliding-window"
}
