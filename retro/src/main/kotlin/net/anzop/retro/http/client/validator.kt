package net.anzop.retro.http.client

import jakarta.validation.ConstraintViolation
import jakarta.validation.Validation
import jakarta.validation.Validator
import jakarta.validation.ValidatorFactory

private val validatorFactory: ValidatorFactory = Validation.buildDefaultValidatorFactory()
private val validator: Validator = validatorFactory.validator

fun <T> validate(obj: T): Set<ConstraintViolation<T>> {
    return validator.validate(obj)
}
