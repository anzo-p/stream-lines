package net.anzop.retro.repository.dynamodb

import java.time.LocalDate
import software.amazon.awssdk.services.dynamodb.model.AttributeValue

typealias AttribMap = Map<String, AttributeValue>

fun String.toAttrib(): AttributeValue =
    AttributeValue.builder().s(this).build()

fun LocalDate.toAttrib(): AttributeValue =
    AttributeValue.builder().s(this.toString()).build()
