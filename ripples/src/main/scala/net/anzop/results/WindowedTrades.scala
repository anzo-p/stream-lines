package net.anzop.results

import com.fasterxml.jackson.annotation.JsonProperty
import net.anzop.processors.WindowedMeasurement
import net.anzop.serdes.{DataSerializer, LocalJsonSerializer}
import org.apache.flink.api.common.serialization.SerializationSchema

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.UUID

case class WindowedTrades(
    @JsonProperty("measure_id") measureId: UUID,
    @JsonProperty("measurement") measurementType: WindowedMeasurement,
    symbol: String,
    @JsonProperty("window_start_time") windowStartTime: OffsetDateTime,
    @JsonProperty("window_end_time") windowEndTime: OffsetDateTime,
    @JsonProperty("record_count") recordCount: Long,
    @JsonProperty("price_at_window_start") priceAtWindowStart: BigDecimal,
    @JsonProperty("min_price") minPrice: BigDecimal,
    @JsonProperty("max_price") maxPrice: BigDecimal,
    @JsonProperty("price_at_window_end") priceAtWindowEnd: BigDecimal,
    @JsonProperty("sum_quantity") sumQuantity: BigDecimal,
    @JsonProperty("sum_notional") sumNotional: BigDecimal,
    @JsonProperty("volume_weighted_avg_price") volumeWeightedAvgPrice: BigDecimal,
    tags: Map[String, String]
  )

object WindowedTrades {

  class InfluxDBSerializer extends DataSerializer[WindowedTrades] with Serializable {

    override def serialize(data: WindowedTrades): String = {
      val tags      = serializeTags(data.tags)
      val timestamp = dateTimeToLong(data.windowEndTime)
      val fields =
        s"""
           |measure_id="${data.measureId.toString}",
           |ticker="${data.symbol}",
           |window_start_time=${dateTimeToLong(data.windowStartTime)}i,
           |window_end_time=${timestamp}i,
           |record_count=${data.recordCount}i,
           |price_at_window_start=${setScale(data.priceAtWindowStart)},
           |min_price=${setScale(data.minPrice)},
           |max_price=${setScale(data.maxPrice)},
           |price_at_window_end=${setScale(data.priceAtWindowEnd)},
           |sum_quantity=${setScale(data.sumQuantity)},
           |sum_notional=${setScale(data.sumNotional)},
           |volume_weighted_avg_price=${setScale(data.volumeWeightedAvgPrice)}
           |""".stripMargin.replaceAll("\n", "")

      s"${data.measurementType.value},$tags $fields $timestamp"
    }
  }

  class JsonSerializerSchema extends SerializationSchema[WindowedTrades] with Serializable with LocalJsonSerializer {

    override def serialize(data: WindowedTrades): Array[Byte] = {
      val json =
        s"""{
           |"measure_id": ${jsString(data.measureId.toString)},
           |"measurement": ${jsString(data.measurementType.value)},
           |"ticker": ${jsString(data.symbol)},
           |"window_start_time": ${jsString(data.windowStartTime.toString)},
           |"window_end_time": ${jsString(data.windowEndTime.toString)},
           |"record_count": ${data.recordCount},
           |"price_at_window_start": ${data.priceAtWindowStart},
           |"min_price": ${data.minPrice},
           |"max_price": ${data.maxPrice},
           |"price_at_window_end": ${data.priceAtWindowEnd},
           |"sum_quantity": ${data.sumQuantity},
           |"sum_notional": ${data.sumNotional},
           |"volume_weighted_avg_price": ${data.volumeWeightedAvgPrice},
           |"tags": ${tagsJson(data.tags)}
           |}""".stripMargin

      json.getBytes(StandardCharsets.UTF_8)
    }
  }
}
