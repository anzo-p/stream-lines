package net.anzop.gather.service

abstract class ThrottlingFetcher(
    val callLimitPerMinute: Int
) {
    fun <T> fetch(
        call: () -> T
    ): T {
        throttle(callLimitPerMinute)
        return call()
    }

    private fun throttle(callLimitPerMinute: Int) =
        Thread.sleep((60 * 1000 / callLimitPerMinute).toLong())
}
