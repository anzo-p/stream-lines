package net.anzop.gather.runner

enum class RunCommandResult(val message: String) {
    ALREADY_RUNNING("Lock acquirable but task is already running."),
    DISPATCHED(""),
    LOCK_UNAVAILABLE("Lock not acquirable. Task likely already in progress."),
    TICKER_NOT_FOUND("Requested ticker not found in the system."),
    ;
}
