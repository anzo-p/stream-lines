package results

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.scala.DefaultScalaModule
import org.apache.flink.api.common.serialization.SerializationSchema
import processors.WindowedVolumesMeasurement

import java.nio.charset.StandardCharsets
import java.time.OffsetDateTime

case class WindowedQuotationVolumes(
    measurementType: WindowedVolumesMeasurement,
    tagBy: Map[String, String],
    windowEndTime: OffsetDateTime,
    sumBidVolume: BigDecimal,
    sumAskVolume: BigDecimal
  )

trait DataSerializer[T] {
  def serialize(data: T): String
}

object WindowedQuotationVolumes {

  class InfluxDBSerializer extends DataSerializer[WindowedQuotationVolumes] with Serializable {
    override def serialize(data: WindowedQuotationVolumes): String = {
      val tags      = data.tagBy.map { case (k, v) => s"$k=$v" }.mkString(",")
      val fields    = s"sumBidVolume=${data.sumBidVolume},sumAskVolume=${data.sumAskVolume}"
      val timestamp = data.windowEndTime.toInstant.getEpochSecond * 1000000000L
      s"${data.measurementType.value},$tags $fields $timestamp"
    }
  }

  class JsonSerializerSchema extends SerializationSchema[WindowedQuotationVolumes] with Serializable {
    private class JsonSerializer extends DataSerializer[WindowedQuotationVolumes] with Serializable {

      import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule

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
