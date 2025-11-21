package net.anzop.results

import com.fasterxml.jackson.annotation.JsonProperty
import net.anzop.processors.WindowedVolumesMeasurement
import net.anzop.serdes.DataSerializer
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

object WindowedQuotationVolumes {

  class InfluxDBSerializer extends DataSerializer[WindowedQuotationVolumes] with Serializable {
    private def setScale(v: BigDecimal): BigDecimal =
      v.setScale(10, BigDecimal.RoundingMode.HALF_UP)

    override def serialize(data: WindowedQuotationVolumes): String = {
      val tags      = data.tags.map { case (k, v) => s"$k=$v" }.mkString(",")
      val timestamp = data.windowEndTime.toInstant.getEpochSecond * 1000000000L
      val fields =
        s"""{
           |"measure_id": ${data.measureId.toString}",
           |"symbol": ${data.symbol}",
           |"window_start_time": ${data.windowStartTime.toInstant.getEpochSecond * 1000000000L},
           |"window_end_time": $timestamp,
           |"sum_bid_volume": ${setScale(data.sumBidVolume)},
           |"sum_ask_volume": ${setScale(data.sumAskVolume)},
           |"record_count": ${data.recordCount},
           |"average_bid"_price": ${setScale(data.averageBidPrice)},
           |"average_ask_price": ${setScale(data.averageAskPrice)},
           |"bid_price_at_window_end": ${setScale(data.bidPriceAtWindowEnd)},
           |"ask_price_at_window_end": ${setScale(data.askPriceAtWindowEnd)}
           |}""".stripMargin.replaceAll("\n", "")
      s"${data.measurementType.value},$tags $fields $timestamp"
    }
  }

  class JsonSerializerSchema extends SerializationSchema[WindowedQuotationVolumes] with Serializable {
    private def escape(s: String): String =
      s.flatMap {
        case '"'  => "\\\""
        case '\\' => "\\\\"
        case '\b' => "\\b"
        case '\f' => "\\f"
        case '\n' => "\\n"
        case '\r' => "\\r"
        case '\t' => "\\t"
        case c    => c.toString
      }

    private def jsString(s: String): String =
      "\"" + escape(s) + "\""

    private def tagsJson(tags: Map[String, String]): String =
      tags
        .map { case (k, v) => jsString(k) + ":" + jsString(v) }
        .mkString("{", ",", "}")

    override def serialize(d: WindowedQuotationVolumes): Array[Byte] = {
      val json =
        s"""{
           |"measure_id": ${jsString(d.measureId.toString)},
           |"measurement": ${jsString(d.measurementType.value)},
           |"symbol": ${jsString(d.symbol)},
           |"window_start_time": ${jsString(d.windowStartTime.toString)},
           |"window_end_time": ${jsString(d.windowEndTime.toString)},
           |"sum_bid_volume": ${d.sumBidVolume},
           |"sum_ask_volume": ${d.sumAskVolume},
           |"record_count": ${d.recordCount},
           |"average_bid_price": ${d.averageBidPrice},
           |"average_ask_price": ${d.averageAskPrice},
           |"bid_price_at_window_end": ${d.bidPriceAtWindowEnd},
           |"ask_price_at_window_end": ${d.askPriceAtWindowEnd},
           |"tags": ${tagsJson(d.tags)}
           |}""".stripMargin

      json.getBytes(StandardCharsets.UTF_8)
    }
  }
}
