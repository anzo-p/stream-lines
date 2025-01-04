package net.anzop.config

import net.anzop.helpers.Extensions.EnvOps

case class RunConfig(dawn: Int, dusk: Int, interval: Int)

object RunConfig {

  val values: RunConfig = {
    val dawn     = sys.env.getOrThrow("RUN_DAWN_HOUR", "RUN_DAWN_HOUR is not set").toInt
    val dusk     = sys.env.getOrThrow("RUN_DUSK_HOUR", "RUN_DUSK_HOUR is not set").toInt
    val interval = sys.env.getOrThrow("RUN_INTERVAL", "RUN_INTERVAL is not set").toInt

    RunConfig(dawn, dusk, interval)
  }
}
