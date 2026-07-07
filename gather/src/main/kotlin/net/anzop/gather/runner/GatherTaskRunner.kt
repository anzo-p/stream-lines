package net.anzop.gather.runner

import org.slf4j.LoggerFactory
import org.springframework.boot.CommandLineRunner
import org.springframework.context.annotation.Profile
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

@Component
@Profile("batch")
class GatherTaskRunner(
    private val appRunner: AppRunner,
    private val environment: Environment,
) : CommandLineRunner {
    private val logger = LoggerFactory.getLogger(GatherTaskRunner::class.java)

    override fun run(vararg args: String) {
        val jobName = environment.getRequiredProperty("GATHER_JOB_NAME")
        val command = when (jobName) {
            "fetch_and_process_index" -> FetchMarketDataAndProcessIndex
            else -> throw IllegalArgumentException("Unsupported or unknown GATHER_JOB_NAME=$jobName")
        }

        logger.info("Starting gather task job=$jobName command=$command")
        when (val result = appRunner.runCommandBlocking(command)) {
            RunCommandResult.DISPATCHED ->
                logger.info("Gather task job succeeded: $jobName")

            else ->
                throw IllegalStateException("Gather task job failed to start: ${result.message}")
        }
    }
}
