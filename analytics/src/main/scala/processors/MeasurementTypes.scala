package processors

sealed trait WindowedVolumesMeasurementType {
  def value: String
}

case object WindowedStockQuotationVolumesMeasurement extends WindowedVolumesMeasurementType {
  override val value = "windowed-stock-quotation-volumes"
}

case object WindowedCryptoQuotationVolumesMeasurement extends WindowedVolumesMeasurementType {
  override val value = "windowed-crypto-quotation-volumes"
}
