package net.anzop.gather.runner

sealed interface RunCommand
data object FetchAndProcessAll : RunCommand
data class FetchFinancials(val ticker: String) : RunCommand
data object RedoIndex : RunCommand
