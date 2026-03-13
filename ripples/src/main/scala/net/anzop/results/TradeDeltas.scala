package net.anzop.results

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.{ObjectMapper, SerializationFeature}
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import net.anzop.processors.{InfluxMeasurement, TradesDeltaMeasurement}
import net.anzop.serdes.DataSerializer
import org.apache.flink.api.common.serialization.SerializationSchema

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.UUID

case class TradeDeltas(
    @JsonProperty("measure_id") measureId: UUID,
    ticker: String,
    @JsonProperty("timestamp") timestamp: OffsetDateTime,
    @JsonProperty("record_count_delta") recordCountDelta: Long,
    @JsonProperty("min_price_delta") minPriceDelta: BigDecimal,
    @JsonProperty("max_price_delta") maxPriceDelta: BigDecimal,
    @JsonProperty("sum_quantity_delta") sumQuantityDelta: BigDecimal,
    @JsonProperty("sum_notional_delta") sumNotionalDelta: BigDecimal,
    @JsonProperty("volume_weighted_avg_price_delta") volumeWeightedAvgPriceDelta: BigDecimal,
    tags: Map[String, String]
  ) extends BaseDelta

object TradeDeltas {
  implicit val influxSerializer: DataSerializer[TradeDeltas]    = new InfluxDBSerializer
  implicit val jsonSerializer: SerializationSchema[TradeDeltas] = new JsonSerializerSchema

  val measurement: InfluxMeasurement = TradesDeltaMeasurement

  private class InfluxDBSerializer extends DataSerializer[TradeDeltas] with Serializable {
    override def serialize(data: TradeDeltas): String = {
      val tags      = serializeTags(data.tags)
      val timestamp = dateTimeToLong(data.timestamp)
      val fields =
        s"""
           |measure_id="${data.measureId.toString}",
           |record_count_delta=${data.recordCountDelta}i,
           |min_price_delta=${setScale(data.minPriceDelta)},
           |max_price_delta=${setScale(data.maxPriceDelta)},
           |sum_quantity_delta=${setScale(data.sumQuantityDelta)},
           |sum_notional_delta=${setScale(data.sumNotionalDelta)},
           |volume_weighted_avg_price_delta=${setScale(data.volumeWeightedAvgPriceDelta)}
           |""".stripMargin.replaceAll("\n", "")

      s"${measurement.value},$tags $fields $timestamp"
    }
  }

  private class JsonSerializerSchema extends SerializationSchema[TradeDeltas] with Serializable {

    @transient private lazy val mapper =
      new ObjectMapper()
        .registerModule(DefaultScalaModule)
        .registerModule(new JavaTimeModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

    override def serialize(data: TradeDeltas): Array[Byte] =
      mapper.writeValueAsString(data).getBytes(StandardCharsets.UTF_8)
  }
}
