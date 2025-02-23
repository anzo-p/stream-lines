package net.anzop.sources.marketData

import java.time.Instant

case class QueryParams(
    bucket: String,
    start: Option[Long] = Some(1L),
    stop: Option[Long]  = Some(Instant.now().getEpochSecond)
  ) {

  final val measurement: String = "ix_reg_arith_d"
}
