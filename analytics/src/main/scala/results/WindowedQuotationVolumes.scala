package results

import com.fasterxml.jackson.databind.ObjectMapper
import org.apache.flink.api.common.serialization.SerializationSchema
import processors.WindowedVolumesMeasurement

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime
import java.util.UUID

case class WindowedQuotationVolumes(
    measureId: UUID,
    measurementType: WindowedVolumesMeasurement,
    symbol: String,
    windowStartTime: OffsetDateTime,
    windowEndTime: OffsetDateTime,
    sumBidVolume: BigDecimal,
    sumAskVolume: BigDecimal,
    tags: Map[String, String]
  )

trait DataSerializer[T] {
  def serialize(data: T): String
}

object WindowedQuotationVolumes {

  class InfluxDBSerializer extends DataSerializer[WindowedQuotationVolumes] with Serializable {
    override def serialize(data: WindowedQuotationVolumes): String = {
      val tags      = data.tags.map { case (k, v) => s"$k=$v" }.mkString(",")
      val timestamp = data.windowEndTime.toInstant.getEpochSecond * 1000000000L
      val fields =
        s"""
           |measureId="${data.measureId.toString}",
           |symbol="${data.symbol}",
           |windowStartTime=${data.windowStartTime.toInstant.getEpochSecond * 1000000000L},
           |windowEndTime=$timestamp,
           |sumBidVolume=${data.sumBidVolume},
           |sumAskVolume=${data.sumAskVolume}
           |""".stripMargin.replaceAll("\n", "")

      s"${data.measurementType.value},$tags $fields $timestamp"
    }
  }

  class JsonSerializerSchema extends SerializationSchema[WindowedQuotationVolumes] with Serializable {
    private class JsonSerializer extends DataSerializer[WindowedQuotationVolumes] with Serializable {
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
