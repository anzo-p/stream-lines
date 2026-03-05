import logging
from dataclasses import dataclass
from datetime import date
from typing import Iterator, List

from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.helpers import compose_default_range
from narwhal.sources.influx.query_result import QueryResult


@dataclass(frozen=True)
class VixData(QueryResult):
    day: date
    value: float


def _flux(bucket: str) -> str:
    return f'''
from(bucket: "{bucket}")
  |> {compose_default_range()}
  |> filter(fn: (r) => r._measurement == "vix-daily")
  |> filter(fn: (r) => r._field == "value")
  |> keep(columns: ["_time", "_value"])
'''.strip()


def vix_query(h: InfluxHandle) -> Iterator[VixData]:
    logger = logging.getLogger(__name__)

    table_list = h.query_api.query(_flux(h.bucket))

    out: List[VixData] = []
    for table in table_list:
        for record in table.records:
            t = record.get_time()
            v = record.get_value()

            if t is None or v is None:
                continue

            out.append(VixData(day=t.date(), value=v))

    logger.info(f"Processed {len(out)} Vix records from InfluxDB query")

    yield from out
