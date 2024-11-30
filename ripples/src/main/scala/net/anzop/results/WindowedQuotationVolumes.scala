package net.anzop.results

import com.fasterxml.jackson.annotation.JsonProperty
import net.anzop.processors.WindowedVolumesMeasurement
import org.apache.flink.api.common.serialization.SerializationSchema

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.UUID

case class WindowedQuotationVolumes(
    @JsonProperty("measure_id") measureId: UUID,
    @JsonProperty("measurement") measurementType: WindowedVolumesMeasurement,
    symbol: String,
    @JsonProperty("window_start_time") windowStartTime: OffsetDateTime,
    @JsonProperty("window_end_time") windowEndTime: OffsetDateTime,
    @JsonProperty("sum_bid_volume") sumBidVolume: BigDecimal,
    @JsonProperty("sum_ask_volume") sumAskVolume: BigDecimal,
    @JsonProperty("record_count") recordCount: Long,
    @JsonProperty("average_bid_price") averageBidPrice: BigDecimal,
    @JsonProperty("average_ask_price") averageAskPrice: BigDecimal,
    @JsonProperty("bid_price_at_window_end") bidPriceAtWindowEnd: BigDecimal,
    @JsonProperty("ask_price_at_window_end") askPriceAtWindowEnd: BigDecimal,
    tags: Map[String, String]
  )

trait DataSerializer[T] {
  def serialize(data: T): String
}

object WindowedQuotationVolumes {

  class InfluxDBSerializer extends DataSerializer[WindowedQuotationVolumes] with Serializable {
    private def setScale(v: BigDecimal): BigDecimal =
      v.setScale(10, BigDecimal.RoundingMode.HALF_UP)

    override def serialize(data: WindowedQuotationVolumes): String = {
      val tags      = data.tags.map { case (k, v) => s"$k=$v" }.mkString(",")
      val timestamp = data.windowEndTime.toInstant.getEpochSecond * 1000000000L
      val fields =
        s"""
           |measure_id="${data.measureId.toString}",
           |symbol="${data.symbol}",
           |window_start_time=${data.windowStartTime.toInstant.getEpochSecond * 1000000000L},
           |window_end_time=$timestamp,
           |sum_bid_volume=${setScale(data.sumBidVolume)},
           |sum_ask_volume=${setScale(data.sumAskVolume)},
           |record_count=${data.recordCount},
           |average_bid_price=${setScale(data.averageBidPrice)},
           |average_ask_price=${setScale(data.averageAskPrice)},
           |bid_price_at_window_end=${setScale(data.bidPriceAtWindowEnd)},
           |ask_price_at_window_end=${setScale(data.askPriceAtWindowEnd)}
           |""".stripMargin.replaceAll("\n", "")
      s"${data.measurementType.value},$tags $fields $timestamp"
    }
  }

  class JsonSerializerSchema extends SerializationSchema[WindowedQuotationVolumes] with Serializable {
    private class JsonSerializer extends DataSerializer[WindowedQuotationVolumes] with Serializable {
      import com.fasterxml.jackson.databind.ObjectMapper
      import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
      import com.fasterxml.jackson.module.scala.DefaultScalaModule

      @transient private lazy val objectMapper = new ObjectMapper()
        .registerModule(DefaultScalaModule)
        .registerModule(new JavaTimeModule())

      override def serialize(data: WindowedQuotationVolumes): String =
        objectMapper.writeValueAsString(data)
    }

    private val jsonSerializer = new JsonSerializer()

    override def serialize(element: WindowedQuotationVolumes): Array[Byte] =
      jsonSerializer.serialize(element).getBytes(StandardCharsets.UTF_8)
  }
}
