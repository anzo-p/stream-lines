import logging
from dataclasses import dataclass
from datetime import date
from typing import Iterator, List

from src.sources.influxdb.client import InfluxHandle


@dataclass(frozen=True)
class VolumeData:
    day: date
    over_moving_avg: float


def _flux(bucket: str, avg_days: int) -> str:
    return f'''
base = from(bucket: "{bucket}")
  |> range(start: time(v: "2016-02-01"), stop: now())
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

    fetch = h.query_api.query(_flux(h.bucket, moving_avg_days))
    logger.info(f"Fetched {len(fetch)} volume data with moving average of {moving_avg_days}")

    out: List[VolumeData] = []
    for table in fetch:
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

    logger.info(f"Writing {len(out)} volume data to InfluxDB")

    yield from out
