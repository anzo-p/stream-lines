package net.anzop.gather.runner

enum class RunnerCallResult(val message: String) {
    ALREADY_RUNNING("Lock acquirable but task is already running."),
    LOCK_UNAVAILABLE("Lock not acquirable. Task likely already in progress."),
    SUCCESS("");
}
