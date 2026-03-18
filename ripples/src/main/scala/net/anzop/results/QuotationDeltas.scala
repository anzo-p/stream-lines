package net.anzop.results

import com.fasterxml.jackson.annotation.JsonProperty
import net.anzop.processors.{InfluxMeasurement, QuotesDeltaMeasurement}
import net.anzop.serdes.DataSerializer

import java.time.OffsetDateTime
import java.util.UUID

case class QuotationDeltas(
    @JsonProperty("measure_id") measureId: UUID,
    ticker: String,
    @JsonProperty("timestamp") timestamp: OffsetDateTime,
    @JsonProperty("record_count_delta") recordCountDelta: Long,
    @JsonProperty("min_ask_price_delta") minAskPriceDelta: BigDecimal,
    @JsonProperty("min_bid_price_delta") minBidPriceDelta: BigDecimal,
    @JsonProperty("max_ask_price_delta") maxAskPriceDelta: BigDecimal,
    @JsonProperty("max_bid_price_delta") maxBidPriceDelta: BigDecimal,
    @JsonProperty("sum_ask_quantity_delta") sumAskQuantityDelta: BigDecimal,
    @JsonProperty("sum_bid_quantity_delta") sumBidQuantityDelta: BigDecimal,
    @JsonProperty("sum_ask_notional_delta") sumAskNotionalDelta: BigDecimal,
    @JsonProperty("sum_bid_notional_delta") sumBidNotionalDelta: BigDecimal,
    @JsonProperty("volume_weighted_avg_ask_price_delta") volumeWeightedAgAskPriceDelta: BigDecimal,
    @JsonProperty("volume_weighted_avg_bid_price_delta") volumeWeightedAvgBidPriceDelta: BigDecimal,
    @JsonProperty("bid_ask_spread_delta") bidAskSpreadDelta: BigDecimal,
    @JsonProperty("spread_midpoint_delta") spreadMidpointDelta: BigDecimal,
    @JsonProperty("order_imbalance_delta") orderImbalanceDelta: BigDecimal,
    tags: Map[String, String]
  ) extends BaseDelta

object QuotationDeltas {
  implicit val influxSerializer: DataSerializer[QuotationDeltas] = new InfluxDBSerializer
  //implicit val jsonSerializer: SerializationSchema[QuotationDeltas] = new JsonSerializerSchema

  val measurement: InfluxMeasurement = QuotesDeltaMeasurement

  private class InfluxDBSerializer extends DataSerializer[QuotationDeltas] with Serializable {
    override def serialize(data: QuotationDeltas): String = {
      val tags      = serializeTags(data.tags)
      val timestamp = dateTimeToLong(data.timestamp)
      val fields =
        s"""
           |measure_id="${data.measureId.toString}",
           |record_count_delta=${data.recordCountDelta}i,
           |min_ask_price_delta=${setScale(data.minAskPriceDelta)},
           |min_bid_price_delta=${setScale(data.minBidPriceDelta)},
           |max_ask_price_delta=${setScale(data.maxAskPriceDelta)},
           |max_bid_price_delta=${setScale(data.maxBidPriceDelta)},
           |sum_ask_quantity_delta=${setScale(data.sumAskQuantityDelta)},
           |sum_bid_quantity_delta=${setScale(data.sumBidQuantityDelta)},
           |sum_ask_notional_delta=${setScale(data.sumAskNotionalDelta)},
           |sum_bid_notional_delta=${setScale(data.sumBidNotionalDelta)},
           |volume_weighted_avg_ask_price_delta=${setScale(data.volumeWeightedAgAskPriceDelta)},
           |volume_weighted_avg_bid_price_delta=${setScale(data.volumeWeightedAvgBidPriceDelta)},
           |bid_ask_spread_delta=${setScale(data.bidAskSpreadDelta)},
           |spread_midpoint_delta=${setScale(data.spreadMidpointDelta)},
           |order_imbalance_delta=${setScale(data.orderImbalanceDelta)}
           |""".stripMargin.replaceAll("\n", "")

      s"${measurement.value},$tags $fields $timestamp"
    }
  }

  /*
  private class JsonSerializerSchema extends SerializationSchema[QuotationDeltas] with Serializable {

    @transient private lazy val mapper =
      new ObjectMapper()
        .registerModule(DefaultScalaModule)
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override def serialize(data: QuotationDeltas): Array[Byte] =
      mapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8)
  }
 */
}
