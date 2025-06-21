package net.anzop.gather.http.server

import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ControllerAdvice
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException

@ControllerAdvice
class ApiExceptionHandler {
    private val logger = LoggerFactory.getLogger(ApiExceptionHandler::class.java)

    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(ex: MethodArgumentTypeMismatchException): HttpResponse =
        badRequest(
            "Bad input on parameter '${ex.name}' of value ${ex.value}. " +
                    "Expected format: ${resolveRequestedFormat(ex.requiredType?.simpleName ?: "unknown")}."
        )

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(ex: MissingServletRequestParameterException): HttpResponse =
        badRequest(
            "Missing required parameter '${ex.parameterName}' of type ${ex.parameterType}."
        )

    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNotFound(): HttpResponse =
        ResponseEntity
            .status(HttpStatus.NOT_FOUND)
            .body(mapOf("error" to "Requested route not found"))

    @ExceptionHandler(Exception::class)
    fun handleGeneric(ex: Exception): HttpResponse =
        ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(mapOf("error" to "The request could not be handled."))
            .also { logger.error("Unhandled exception", ex) }

    private fun resolveRequestedFormat(paramType: String) =
        when (paramType) {
            "LocalDate" -> "YYYY-MM-DD"
            else -> this // fill in as you go
        }

    private fun badRequest(message: String): HttpResponse =
        ResponseEntity
            .badRequest()
            .body(mapOf("error" to message))
            .also { logger.warn(message) }
}
