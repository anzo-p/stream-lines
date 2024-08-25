package net.anzop.retro.repository.dynamodb

import java.time.LocalDate
import net.anzop.retro.model.IndexMember
import net.anzop.retro.model.IndexMembers
import net.anzop.retro.model.PrevDayData
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
            "prevDayData" to AttributeValue.builder().m(
                mapOf(
                    "date" to this.prevDayData.date.toAttrib(),
                    "avgPrice" to this.prevDayData.avgPrice.toAttrib()
                )
            ).build()
        )
    ).build()

fun IndexMembers.toAttrib(): AttributeValue =
    AttributeValue.builder().m(
        this.mapValues { it.value.toAttrib() }
    ).build()

fun AttributeValue?.toIntOrDefault(default: Int = 0): Int =
    this?.n()?.toIntOrNull() ?: default

fun AttributeValue?.toDoubleOrDefault(default: Double = 0.0): Double =
    this?.n()?.toDoubleOrNull() ?: default

fun AttributeValue?.toStringOrDefault(default: String = ""): String =
    this?.s() ?: default

fun AttributeValue?.toMapOrDefault(default: AttribMap = emptyMap()): AttribMap =
    this?.m() ?: default

fun getMemberSecurities(item: AttribMap): IndexMembers =
    convertAttributeValueMap(
        item["memberSecurities"].toMapOrDefault(),
        ::attribToMemberSecurity
    ).toMutableMap()

fun attribToMemberSecurity(attributes: AttribMap): IndexMember {
    val prevDayData = attributes["prevDayData"].toMapOrDefault().let {
        PrevDayData(
            date = LocalDate.parse(it["date"].toStringOrDefault()),
            avgPrice = it["avgPrice"].toDoubleOrDefault()
        )
    }

    return IndexMember(
        ticker = attributes["ticker"].toStringOrDefault(),
        measurement = Measurement.fromCode(attributes["measurement"].toStringOrDefault()),
        indexValueWhenIntroduced = attributes["indexValueWhenIntroduced"].toDoubleOrDefault(),
        introductionPrice = attributes["introductionPrice"].toDoubleOrDefault(),
        prevDayData = prevDayData
    )
}

fun <T> convertAttributeValueMap(
    attributes: AttribMap,
    converter: (AttribMap) -> T
): Map<String, T> =
    attributes.mapValues { (_, value) ->
        converter(value.m())
    }
