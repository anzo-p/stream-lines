package net.anzop

import com.amazonaws.services.kinesis.model.{AccessDeniedException, LimitExceededException, ResourceNotFoundException}
import net.anzop.config.{InfluxDetails, KinesisProps}
import net.anzop.helpers.StreamHelpers
import net.anzop.streams.MarketData
import org.slf4j.{Logger, LoggerFactory}

object Ripples {
  val logger: Logger = LoggerFactory.getLogger(getClass)

  def main(args: Array[String]): Unit = {
    logger.info("Application starting")

    StreamHelpers.checkInfluxDB()
    val kinesisProps = KinesisProps.make()

    try {
      MarketData.stream(InfluxDetails.make(), kinesisProps)
    } catch {
      case e: ResourceNotFoundException =>
        logger.error(s"Kinesis stream ${kinesisProps.getOrDefault("streamName", "")} not found: ${e.getMessage}")

      case e: AccessDeniedException =>
        logger.error(s"Insufficient privileges for associated AWS Role to execute on Kinesis: ${e.getMessage}")

      case e: LimitExceededException =>
        logger.error(s"Attempt to exceed limits imposed by Kinesis: ${e.getMessage}")

      case e: Throwable =>
        logger.error(s"${e.getMessage}, ${e.getStackTrace.mkString("\n")}, ${e.getCause}}")
    }
  }
}
