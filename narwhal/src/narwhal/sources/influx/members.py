import logging
from dataclasses import dataclass
from datetime import date
from typing import Iterator, List

from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.helpers import compose_default_range

TOP_TICKERS_COUNT = 40


@dataclass(frozen=True)
class MemberData:
    day: date
    daily_spread: float


# how spread out are the daily changes in value?
# the assumption is that during bull markets prices tend to increase more together
# and when market players are de-risking their price awareness for individual stocks increase
# this would cause the daily change of valuations to spread out more
def _flux(bucket: str) -> str:
    return f'''
import "math"
import "influxdata/influxdb/schema"

topTickers = from(bucket: "{bucket}")
  |> range(start: -15d)
  |> filter(fn: (r) => r._measurement == "securities-daily-change-extended-hours")
  |> filter(fn: (r) => r._field == "totalTradingValue")
  |> group(columns: ["ticker"])
  |> sum()
  |> group()
  |> top(n: {TOP_TICKERS_COUNT}, columns: ["_value"])
  |> findColumn(fn: (key) => true, column: "ticker")

priceChangePercentage = (r) => (r.priceChangeAvg - r.prevPriceChangeAvg) / r.prevPriceChangeAvg * 100.0

base = from(bucket: "{bucket}")
  |> {compose_default_range()}
  |> filter(fn: (r) => r._measurement == "securities-daily-change-extended-hours")
  |> filter(fn: (r) => r._field == "priceChangeAvg" or r._field == "prevPriceChangeAvg")
  |> filter(fn: (r) => contains(value: r.ticker, set: topTickers))
  |> keep(columns: ["_time", "_field", "_value", "ticker"])
  |> schema.fieldsAsCols()
  |> map(fn: (r) => ({{ r with _value: priceChangePercentage(r) }}))
  |> keep(columns: ["_time", "_value"])

data = base
  |> group(columns: ["_time"])

union(tables:[
  data |> quantile(q: 0.10) |> set(key:"q", value:"q10"),
  data |> quantile(q: 0.25) |> set(key:"q", value:"q25"),
  data |> quantile(q: 0.75) |> set(key:"q", value:"q75"),
  data |> quantile(q: 0.90) |> set(key:"q", value:"q90")
])
  |> pivot(rowKey: ["_time"], columnKey: ["q"], valueColumn: "_value")
  |> map(fn: (r) => ({{
      _time: r._time,
      // average spread of the middle 50% and 80% daily changes
      spread: ((r.q75 - r.q25) + (r.q90 - r.q10)) / 2.0
    }}))
'''.strip()


def member_query(h: InfluxHandle) -> Iterator[MemberData]:
    logger = logging.getLogger(__name__)

    table_list = h.query_api.query(_flux(h.bucket))
    logger.info(
        f"Fetched member data for top {TOP_TICKERS_COUNT} tickers by trading value in the last 15 days"
    )

    out: List[MemberData] = []
    for table in table_list:
        for record in table.records:
            t = record.get_time()
            v = record["spread"]

            if t is None:
                continue

            out.append(MemberData(day=t.date(), daily_spread=v))

    logger.info(f"Fetched {len(out)} member data from InfluxDB")

    yield from out
