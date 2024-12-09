package net.anzop.sources

import net.anzop.config.InfluxDetails
import net.anzop.models.MarketData
import org.apache.flink.streaming.api.functions.source.SourceFunction

class IndexDataSource(influxDetails: InfluxDetails) extends SourceFunction[MarketData] {
  @volatile private var running                                 = true
  @transient private var influxResourceInstance: InfluxResource = _

  override def run(ctx: SourceFunction.SourceContext[MarketData]): Unit =
    try {
      influxResourceInstance = new InfluxResource(influxDetails)

      while (running) {
        influxResourceInstance
          .fetchIndexData()
          .foreach(ctx.collect)

        Thread.sleep(1000 * 60 * 60) // into app config as pollInterval
      }
    } finally {
      running = false
      influxResourceInstance.close()
    }

  override def cancel(): Unit = running = false
}
