package net.anzop.gather.helpers

object StringHelpers {

    fun coalesceOrThrow(vararg maybeValues: String?, msg: String): String =
        maybeValues.firstOrNull { it != null } ?: throw IllegalArgumentException(msg)
}
