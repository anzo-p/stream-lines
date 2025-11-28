package net.anzop.gather.repository.dynamodb

import java.time.LocalDate
import net.anzop.gather.config.AwsConfig
import net.anzop.gather.model.IndexMember
import net.anzop.gather.model.MutableIndexMembers
import net.anzop.gather.model.PrevDayData
import net.anzop.gather.model.marketData.Measurement
import org.springframework.stereotype.Repository
import software.amazon.awssdk.services.dynamodb.DynamoDbClient
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

@Repository
class IndexMemberRepository(
    awsConfig: AwsConfig,
    dynamoDbClient: DynamoDbClient,
) : DynamoDbCore(awsConfig, dynamoDbClient) {

    fun storeMemberSecurities(measurement: Measurement, securities: MutableIndexMembers) =
        saveItem(
            mapOf(
                "pk" to "memberSecurity".toAttrib(),
                "sk" to "measurement#${measurement.code}".toAttrib(),
                "memberSecurities" to securities.toAttrib()
            )
        )

    fun getMemberSecurities(measurement: Measurement): MutableIndexMembers {
        val key = mapOf(
            "pk" to "memberSecurity".toAttrib(),
            "sk" to "measurement#${measurement.code}".toAttrib(),
        )
        return getItem(key)
            ?.let { parseMemberSecurities(it) }
            ?: mutableMapOf()
    }

    fun deleteMemberSecurities(measurement: Measurement) =
        deleteItem(
            mapOf(
                "pk" to "memberSecurity".toAttrib(),
                "sk" to "measurement#${measurement.code}".toAttrib(),
            )
        )

    private fun IndexMember.toAttrib(): AttributeValue =
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

    private fun MutableIndexMembers.toAttrib(): AttributeValue =
        AttributeValue.builder().m(
            this.mapValues { it.value.toAttrib() }
        ).build()

    private fun parseMemberSecurities(attribs: AttribMap): MutableIndexMembers =
        convertAttributeValueMap(
            attribs["memberSecurities"].toMapOrDefault(),
            ::attribToMemberSecurity
        )
            .filterValues { it != null }
            .mapValues { it.value!! }
            .toMutableMap()

    private fun attribToMemberSecurity(attribs: AttribMap): IndexMember {
        val prevDayData = attribs["prevDayData"].toMapOrDefault().let {
            PrevDayData(
                date = LocalDate.parse(it["date"].toStringOrDefault()),
                avgPrice = it["avgPrice"].toDoubleOrDefault()
            )
        }

        return IndexMember(
            ticker = attribs["ticker"].toStringOrDefault(),
            measurement = Measurement.fromCode(attribs["measurement"].toStringOrDefault()),
            indexValueWhenIntroduced = attribs["indexValueWhenIntroduced"].toDoubleOrDefault(),
            introductionPrice = attribs["introductionPrice"].toDoubleOrDefault(),
            prevDayData = prevDayData
        )
    }
}
