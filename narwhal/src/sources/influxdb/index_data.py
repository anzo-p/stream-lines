import logging
from dataclasses import dataclass
from datetime import date
from typing import Iterator, List

from .client import InfluxHandle


@dataclass(frozen=True)
class IndexData:
    day: date
    over_moving_avg: float
    over_kaufman_avg: float


def _flux(bucket: str, avg_days: int) -> str:
    return f'''
import "math"

base = from(bucket: "{bucket}")
  |> range(start: time(v: "2016-02-01"), stop: now())
  |> filter(fn: (r) => r._measurement == "index-daily-change-regular-hours")
  |> filter(fn: (r) => r._field == "priceChangeHigh")
  |> map(fn: (r) => ({{ r with _value: math.log(x: r._value) }}))
  |> keep(columns: ["_time", "_value", "regularTradingHours"])
  |> group(columns: ["regularTradingHours"])

index = base
  |> set(key: "_field", value: "index")

movingAvg = base
  |> movingAverage(n: {avg_days})
  |> set(key: "_field", value: "movingAvg")

kaufmanAvg = base
  |> kaufmansAMA(n: {avg_days})
  |> set(key: "_field", value: "kaufmanAvg")

union(tables: [index, movingAvg, kaufmanAvg])
  |> pivot(
    rowKey: ["_time", "regularTradingHours"],
    columnKey: ["_field"],
    valueColumn: "_value",
  )
  |> filter(fn: (r) => exists r.movingAvg and exists r.kaufmanAvg )
'''.strip()


def index_query(h: InfluxHandle, moving_avg_days: int) -> Iterator[IndexData]:
    logger = logging.getLogger(__name__)

    fetch = h.query_api.query(_flux(h.bucket, moving_avg_days))
    logger.info(f"Fetched index data with moving average window of {moving_avg_days} days")

    out: List[IndexData] = []
    for table in fetch:
        for record in table.records:
            t = record.get_time()
            v = record["index"]
            ma = record["movingAvg"]
            ka = record["kaufmanAvg"]

            if t is None or ma is None or ka is None:
                continue

            out.append(IndexData(day=t.date(), over_moving_avg=v / ma, over_kaufman_avg=v / ka))

    logger.info(f"Parsed {len(out)} index data points from InfluxDB query")

    yield from out
