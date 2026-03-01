import logging
from dataclasses import dataclass
from datetime import date
from typing import Iterator, List

from influxdb_client.client.flux_table import TableList

from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.helpers import compose_default_range
from narwhal.sources.influx.query_result import QueryResult


@dataclass(frozen=True)
class IndexData(QueryResult):
    day: date
    over_moving_avg: float
    over_kaufman_avg: float


def _flux(bucket: str, avg_days: int) -> str:
    return f'''
import "math"

base = from(bucket: "{bucket}")
  |> {compose_default_range()}
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

    table_ist: TableList = h.query_api.query(_flux(h.bucket, moving_avg_days))

    out: List[IndexData] = []
    for table in table_ist:
        for record in table.records:
            t = record.get_time()
            v = record["index"]
            ma = record["movingAvg"]
            ka = record["kaufmanAvg"]

            if t is None or ma is None or ka is None:
                continue

            out.append(IndexData(day=t.date(), over_moving_avg=v / ma, over_kaufman_avg=v / ka))

    logger.info(
        f"Processed {len(out)} index data records from InfluxDB query, "
        f"using moving average of {moving_avg_days} bank days "
        f"({moving_avg_days * 1.4} actual days)"
    )

    yield from out
