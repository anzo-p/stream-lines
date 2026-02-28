import logging
from dataclasses import dataclass
from datetime import date
from typing import Iterator, List

from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.helpers import compose_default_range
from narwhal.sources.influx.query_result import QueryResult


@dataclass(frozen=True)
class VolumeData(QueryResult):
    day: date
    over_moving_avg: float


def _flux(bucket: str, avg_days: int) -> str:
    return f'''
base = from(bucket: "{bucket}")
  |> {compose_default_range()}
  |> filter(fn: (r) => r._measurement == "index-daily-change-regular-hours")
  |> filter(fn: (r) => r._field == "totalTradingValue")
  |> keep(columns: ["_time", "_value"])

dollarValue = base
  |> rename(columns: {{_value: "dollarValue"}})

movingAvg = base
  |> movingAverage(n: {avg_days})
  |> rename(columns: {{_value: "movingAvg"}})

join(
  tables: {{dv: dollarValue, ma: movingAvg}},
  on: ["_time"],
  method: "inner"
)
  |> map(fn: (r) => ({{
      _time: r._time,
      _value: r.dollarValue / r.movingAvg
  }}))
'''.strip()


def volume_query(h: InfluxHandle, moving_avg_days: int) -> Iterator[VolumeData]:
    logger = logging.getLogger(__name__)

    table_list = h.query_api.query(_flux(h.bucket, moving_avg_days))

    out: List[VolumeData] = []
    for table in table_list:
        for record in table.records:
            t = record.get_time()
            v = record.get_value()

            if t is None or v is None:
                continue

            out.append(
                VolumeData(
                    day=t.date(),
                    over_moving_avg=v,
                )
            )

    logger.info(
        f"Processed {len(out)} volume data records from InfluxDB query, "
        f"using moving average of {moving_avg_days}"
    )

    yield from out
