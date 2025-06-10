package net.anzop.gather.helpers

object StringHelpers {

    fun xorNotNull(a: String?, b: String?, msg: String): String =
        with(require((a ?: b) != null) { msg }) {
            (a ?: b)!!
        }
}
