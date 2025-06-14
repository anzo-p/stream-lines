package net.anzop.gather.helpers.jakarta

import jakarta.validation.Validation

inline fun <reified T> validate(obj: T) {
    Validation
        .buildDefaultValidatorFactory()
        .validator
        .validate(obj)
        .takeIf { it.isNotEmpty() }
        ?.let { violations ->
            val errorMessages = violations.joinToString("; ") { violation ->
                val propertyName = violation.propertyPath.iterator().asSequence().last().name
                "'$propertyName' ${violation.message}"
            }
            throw IllegalArgumentException("Validation failed for ${T::class.simpleName}: $errorMessages")
        }
}
