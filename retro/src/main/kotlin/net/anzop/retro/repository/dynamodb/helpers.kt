package net.anzop.retro.repository.dynamodb

import java.time.LocalDate
import net.anzop.retro.model.IndexMember
import net.anzop.retro.model.marketData.Measurement
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

typealias AttribMap = Map<String, AttributeValue>

fun Number.toAttrib(): AttributeValue =
    AttributeValue.builder().n(this.toString()).build()

fun String.toAttrib(): AttributeValue =
    AttributeValue.builder().s(this).build()

fun LocalDate.toAttrib(): AttributeValue =
    AttributeValue.builder().s(this.toString()).build()

fun IndexMember.toAttrib(): AttributeValue =
    AttributeValue.builder().m(
        mapOf(
            "ticker" to this.ticker.toAttrib(),
            "measurement" to this.measurement.code.toAttrib(),
            "indexValueWhenIntroduced" to this.indexValueWhenIntroduced.toAttrib(),
            "introductionPrice" to this.introductionPrice.toAttrib(),
            "prevDayPrice" to this.prevDayPrice.toAttrib(),
        )
    ).build()

fun Map<String, IndexMember>.toAttrib(): AttributeValue {
    val securityMap = this.mapValues { it.value.toAttrib() }
    return AttributeValue.builder().m(securityMap).build()
}

fun AttributeValue?.toDoubleOrDefault(default: Double = 0.0): Double {
    return this?.n()?.toDoubleOrNull() ?: default
}

fun AttributeValue?.toStringOrDefault(default: String = ""): String {
    return this?.s() ?: default
}

fun AttributeValue?.toIntOrDefault(default: Int = 0): Int {
    return this?.n()?.toIntOrNull() ?: default
}

fun AttributeValue?.toMapOrDefault(default: AttribMap = emptyMap()): AttribMap {
    return this?.m() ?: default
}

fun getMemberSecurities(item: AttribMap): Map<String, IndexMember> =
    convertAttributeValueMap(
        item["memberSecurities"].toMapOrDefault(),
        ::attribToMemberSecurity
    )

fun attribToMemberSecurity(attributes: AttribMap): IndexMember =
    IndexMember(
        ticker = attributes["ticker"].toStringOrDefault(),
        measurement = Measurement.fromCode(attributes["measurement"].toStringOrDefault()),
        indexValueWhenIntroduced = attributes["indexValueWhenIntroduced"].toDoubleOrDefault(),
        introductionPrice = attributes["introductionPrice"].toDoubleOrDefault(),
        prevDayPrice = attributes["prevDayPrice"].toDoubleOrDefault(),
    )

fun <T> convertAttributeValueMap(
    attributes: AttribMap,
    converter: (AttribMap) -> T
): Map<String, T> {
    return attributes.mapValues { (_, value) ->
        converter(value.m())
    }
}
