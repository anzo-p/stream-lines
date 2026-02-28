import logging
from collections import deque
from dataclasses import dataclass, replace
from datetime import date
from typing import Iterator, List, Deque

from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.helpers import compose_default_range
from narwhal.sources.influx.query_result import QueryResult


@dataclass(frozen=True)
class DrawdownData(QueryResult):
    day: date
    current_drawdown: float
    days_since_dip: int
    fwd_max_drawdown: float


def _flux(bucket: str) -> str:
    return f'''
from(bucket: "{bucket}")
  |> {compose_default_range()}
  |> filter(fn: (r) => r._measurement == "drawdown-analysis")
  |> filter(fn: (r) => r._field == "drawdownLow")
  |> keep(columns: ["_time", "_value"])
'''.strip()


def _add_days_since_dip(rows: list[DrawdownData]) -> list[DrawdownData]:
    rows = sorted(rows, key=lambda r: r.day)
    out: list[DrawdownData] = []
    days_since_dip = 0

    for row in rows:
        days_since_dip = 0 if row.current_drawdown <= 95 else days_since_dip + 1
        out.append(replace(row, days_since_dip=days_since_dip))

    return out


def _add_fwd_max_drawdown(rows: list[DrawdownData], future_window_days: int) -> list[DrawdownData]:
    rows = sorted(rows, key=lambda r: r.day)
    levels = [r.current_drawdown for r in rows]
    row_count = len(rows)
    dq: Deque[int] = deque()

    def push(idx: int) -> None:
        while dq and levels[dq[-1]] >= levels[idx]:
            dq.pop()
        dq.append(idx)

    # prefill indices 1..n
    for idx in range(1, min(row_count, future_window_days + 1)):
        push(idx)

    out = []
    for i in range(row_count):
        # remove indices that are no longer in the future window
        while dq and dq[0] < i + 1:
            dq.popleft()

        # use the current max in the deque if it exists
        fwd_max = levels[dq[0]] if dq else levels[i]
        out.append(replace(rows[i], fwd_max_drawdown=fwd_max))

        nxt = i + future_window_days + 1
        if nxt < row_count:
            push(nxt)

    return out


def drawdown_query(h: InfluxHandle, fwd_bank_days: int) -> Iterator[DrawdownData]:
    logger = logging.getLogger(__name__)

    table_list = h.query_api.query(_flux(h.bucket))

    out: List[DrawdownData] = []
    for table in table_list:
        for record in table.records:
            t = record.get_time()
            v = record.get_value()

            if t is None or v is None:
                continue

            out.append(
                DrawdownData(
                    day=t.date(), current_drawdown=float(v), days_since_dip=0, fwd_max_drawdown=0.0
                )
            )

    out = _add_days_since_dip(out)
    logger.info(f"Applied bull market streaks")

    out = _add_fwd_max_drawdown(out, fwd_bank_days)
    logger.info(f"Applied forward max drawdown")

    logger.info(
        f"Processed {len(out)} volume data records from InfluxDB query, "
        f"using forward bank days of {fwd_bank_days}"
    )

    yield from out
