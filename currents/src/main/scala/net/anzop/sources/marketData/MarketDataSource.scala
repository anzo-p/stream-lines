package net.anzop.sources.marketData

import net.anzop.config.InfluxDetails
import net.anzop.models.MarketData
import org.apache.flink.streaming.api.functions.source.SourceFunction

class MarketDataSource(influxDetails: InfluxDetails) extends SourceFunction[MarketData] {
  @volatile private var running           = true
  @transient private var dbConn: InfluxDB = _

  override def run(ctx: SourceFunction.SourceContext[MarketData]): Unit =
    try {
      dbConn = new InfluxDB(influxDetails)

      while (running) {
        dbConn
          .requestData[MarketData](IndexData)
          .foreach(ctx.collect)

        Thread.sleep(1000 * 60 * 60) // into app config as pollInterval
      }
    } finally {
      running = false
      dbConn.close()
    }

  override def cancel(): Unit = running = false
}
