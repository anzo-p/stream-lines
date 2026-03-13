package net.anzop.results

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import net.anzop.processors.{InfluxMeasurement, WindowedQuotesMeasurement}
import net.anzop.serdes.DataSerializer
import org.apache.flink.api.common.serialization.SerializationSchema

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.UUID

case class WindowedQuotations(
    @JsonProperty("measure_id") measureId: UUID,
    ticker: String,
    @JsonProperty("window_start_time") windowStartTime: OffsetDateTime,
    @JsonProperty("window_end_time") timestamp: OffsetDateTime,
    @JsonProperty("record_count") recordCount: Long,
    @JsonProperty("min_ask_price") minAskPrice: BigDecimal,
    @JsonProperty("min_bid_price") minBidPrice: BigDecimal,
    @JsonProperty("max_ask_price") maxAskPrice: BigDecimal,
    @JsonProperty("max_bid_price") maxBidPrice: BigDecimal,
    @JsonProperty("sum_ask_quantity") sumAskQuantity: BigDecimal,
    @JsonProperty("sum_bid_quantity") sumBidQuantity: BigDecimal,
    @JsonProperty("sum_ask_notional") sumAskNotional: BigDecimal,
    @JsonProperty("sum_bid_notional") sumBidNotional: BigDecimal,
    @JsonProperty("volume_weighted_avg_ask_price") volumeWeightedAgAskPrice: BigDecimal,
    @JsonProperty("volume_weighted_avg_bid_price") volumeWeightedAvgBidPrice: BigDecimal,
    @JsonProperty("bid_ask_spread") bidAskSpread: BigDecimal,
    @JsonProperty("spread_midpoint") spreadMidpoint: BigDecimal,
    @JsonProperty("order_imbalance") orderImbalance: BigDecimal,
    tags: Map[String, String]
  ) extends BaseWindow

object WindowedQuotations {
  implicit val influxSerializer: DataSerializer[WindowedQuotations]    = new InfluxDBSerializer
  implicit val jsonSerializer: SerializationSchema[WindowedQuotations] = new JsonSerializerSchema

  val measurement: InfluxMeasurement = WindowedQuotesMeasurement

  private class InfluxDBSerializer extends DataSerializer[WindowedQuotations] with Serializable {
    override def serialize(data: WindowedQuotations): String = {
      val tags      = serializeTags(data.tags)
      val timestamp = dateTimeToLong(data.timestamp)
      val fields =
        s"""
           |measure_id="${data.measureId.toString}",
           |window_start_time=${dateTimeToLong(data.windowStartTime)}i,
           |window_end_time=${timestamp}i,
           |record_count=${data.recordCount}i,
           |min_ask_price=${setScale(data.minAskPrice)},
           |min_bid_price=${setScale(data.minBidPrice)},
           |max_ask_price=${setScale(data.maxAskPrice)},
           |max_bid_price=${setScale(data.maxBidPrice)},
           |sum_ask_quantity=${setScale(data.sumAskQuantity)},
           |sum_bid_quantity=${setScale(data.sumBidQuantity)},
           |sum_ask_notional=${setScale(data.sumAskNotional)},
           |sum_bid_notional=${setScale(data.sumBidNotional)},
           |volume_weighted_avg_ask_price=${setScale(data.volumeWeightedAgAskPrice)},
           |volume_weighted_avg_bid_price=${setScale(data.volumeWeightedAvgBidPrice)},
           |bid_ask_spread=${setScale(data.bidAskSpread)},
           |spread_midpoint=${setScale(data.spreadMidpoint)},
           |order_imbalance=${setScale(data.orderImbalance)}
           |""".stripMargin.replaceAll("\n", "")

      s"${measurement.value},$tags $fields $timestamp"
    }
  }

  private class JsonSerializerSchema extends SerializationSchema[WindowedQuotations] with Serializable {

    @transient private lazy val mapper =
      new ObjectMapper()
        .registerModule(DefaultScalaModule)
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override def serialize(data: WindowedQuotations): Array[Byte] =
      mapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8)
  }
}
