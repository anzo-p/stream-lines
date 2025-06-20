package net.anzop.gather.helpers.jakarta

import jakarta.validation.Constraint
import jakarta.validation.Payload
import kotlin.reflect.KClass

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [UniqueSymbolValidator::class])
annotation class UniqueSymbols(
    val message: String = "Ticker symbols must be unique",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
@Constraint(validatedBy = [UniqueCompanyValidator::class])
annotation class UniqueCompany(
    val message: String = "Combination of company name and series must be unique",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)

@Constraint(validatedBy = [AtLeastOneNonNullValidator::class])
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.RUNTIME)
annotation class AtLeastOneNonNull(
    val message: String = "At least one of annualData or quarterlyData must be non-null",
    val groups: Array<KClass<*>> = [],
    val payload: Array<KClass<out Payload>> = []
)
