package net.anzop.results

import com.fasterxml.jackson.annotation.JsonProperty
import net.anzop.processors.{InfluxMeasurement, WindowedQuotesMeasurement}
import net.anzop.serdes.{DataSerializer, LocalJsonSerializer}
import org.apache.flink.api.common.serialization.SerializationSchema

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.UUID

case class WindowedQuotes(
    @JsonProperty("measure_id") measureId: UUID,
    ticker: String,
    @JsonProperty("window_start_time") windowStartTime: OffsetDateTime,
    @JsonProperty("window_end_time") timestamp: OffsetDateTime,
    @JsonProperty("record_count") recordCount: Long,
    @JsonProperty("ask_price_at_window_start") askPriceAtWindowStart: BigDecimal,
    @JsonProperty("bid_price_at_window_start") bidPriceAtWindowStart: BigDecimal,
    @JsonProperty("min_ask_price") minAskPrice: BigDecimal,
    @JsonProperty("min_bid_price") minBidPrice: BigDecimal,
    @JsonProperty("max_ask_price") maxAskPrice: BigDecimal,
    @JsonProperty("max_bid_price") maxBidPrice: BigDecimal,
    @JsonProperty("ask_price_at_window_end") askPriceAtWindowEnd: BigDecimal,
    @JsonProperty("bid_price_at_window_end") bidPriceAtWindowEnd: BigDecimal,
    @JsonProperty("sum_ask_quantity") sumAskQuantity: BigDecimal,
    @JsonProperty("sum_bid_quantity") sumBidQuantity: BigDecimal,
    @JsonProperty("sum_ask_notional") sumAskNotional: BigDecimal,
    @JsonProperty("sum_bid_notional") sumBidNotional: BigDecimal,
    @JsonProperty("volume_weighted_avg_ask_price") volumeWeightedAgAskPrice: BigDecimal,
    @JsonProperty("volume_weighted_avg_bid_price") volumeWeightedAvgBidPrice: BigDecimal,
    @JsonProperty("bid_ask_spread") bidAskSpread: BigDecimal,
    @JsonProperty("spread_midpoint") spreadMidpoint: BigDecimal,
    @JsonProperty("order_book_imbalance") orderBookImbalance: BigDecimal,
    tags: Map[String, String]
  ) extends BaseWindow

object WindowedQuotes {
  implicit val influxSerializer: DataSerializer[WindowedQuotes]    = new InfluxDBSerializer
  implicit val jsonSerializer: SerializationSchema[WindowedQuotes] = new JsonSerializerSchema

  val measurement: InfluxMeasurement = WindowedQuotesMeasurement

  private class InfluxDBSerializer extends DataSerializer[WindowedQuotes] with Serializable {
    override def serialize(data: WindowedQuotes): String = {
      val tags      = serializeTags(data.tags)
      val timestamp = dateTimeToLong(data.timestamp)
      val fields =
        s"""
           |measure_id="${data.measureId.toString}",
           |ticker="${data.ticker}",
           |window_start_time=${dateTimeToLong(data.windowStartTime)}i,
           |window_end_time=${timestamp}i,
           |record_count=${data.recordCount}i,
           |ask_price_at_window_start=${setScale(data.askPriceAtWindowStart)},
           |bid_price_at_window_start=${setScale(data.bidPriceAtWindowStart)},
           |min_ask_price=${setScale(data.minAskPrice)},
           |min_bid_price=${setScale(data.minBidPrice)},
           |max_ask_price=${setScale(data.maxAskPrice)},
           |max_bid_price=${setScale(data.maxBidPrice)},
           |ask_price_at_window_end=${setScale(data.askPriceAtWindowEnd)},
           |bid_price_at_window_end=${setScale(data.bidPriceAtWindowEnd)},
           |sum_ask_quantity=${setScale(data.sumAskQuantity)},
           |sum_bid_quantity=${setScale(data.sumBidQuantity)},
           |sum_ask_notional=${setScale(data.sumAskNotional)},
           |sum_bid_notional=${setScale(data.sumBidNotional)},
           |volume_weighted_avg_ask_price=${setScale(data.volumeWeightedAgAskPrice)},
           |volume_weighted_avg_bid_price=${setScale(data.volumeWeightedAvgBidPrice)},
           |bid_ask_spread=${setScale(data.bidAskSpread)},
           |spread_midpoint=${setScale(data.spreadMidpoint)},
           |order_book_imbalance=${setScale(data.orderBookImbalance)}
           |""".stripMargin.replaceAll("\n", "")

      s"${measurement.value},$tags $fields $timestamp"
    }
  }

  private class JsonSerializerSchema extends SerializationSchema[WindowedQuotes] with Serializable with LocalJsonSerializer {
    override def serialize(data: WindowedQuotes): Array[Byte] = {
      val json =
        s"""{
           |"measure_id": ${jsString(data.measureId.toString)},
           |"measurement": ${jsString(measurement.value)},
           |"ticker": ${jsString(data.ticker)},
           |"window_start_time": ${jsString(data.windowStartTime.toString)},
           |"window_end_time": ${jsString(data.timestamp.toString)},
           |"record_count": ${data.recordCount},
           |"ask_price_at_window_start": ${data.askPriceAtWindowStart},
           |"bid_price_at_window_start": ${data.bidPriceAtWindowStart},
           |"min_ask_price": ${data.minAskPrice},
           |"min_bid_price": ${data.minBidPrice},
           |"max_ask_price": ${data.maxAskPrice},
           |"max_bid_price": ${data.maxBidPrice},
           |"ask_price_at_window_end": ${data.askPriceAtWindowEnd},
           |"bid_price_at_window_end": ${data.bidPriceAtWindowEnd},
           |"sum_ask_quantity": ${data.sumAskQuantity},
           |"sum_bid_quantity": ${data.sumBidQuantity},
           |"sum_ask_notional": ${data.sumAskNotional},
           |"sum_bid_notional": ${data.sumBidNotional},
           |"volume_weighted_avg_ask_price": ${data.volumeWeightedAgAskPrice},
           |"volume_weighted_avg_bid_price": ${data.volumeWeightedAvgBidPrice},
           |"bid_ask_spread": ${data.bidAskSpread},
           |"spread_midpoint": ${data.spreadMidpoint},
           |"order_book_imbalance": ${data.orderBookImbalance},
           |"tags": ${tagsJson(data.tags)}
           |}""".stripMargin

      json.getBytes(StandardCharsets.UTF_8)
    }
  }
}
