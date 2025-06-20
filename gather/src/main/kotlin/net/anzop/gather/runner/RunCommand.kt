package net.anzop.gather.runner

import java.time.Instant

sealed interface RunCommand
data class DeleteMarketData(val ticker: String, val since: Instant) : RunCommand
data object FetchMarketDataAndProcessIndex : RunCommand
data class FetchFinancials(val ticker: String) : RunCommand
data object RedoIndex : RunCommand
