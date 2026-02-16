import logging
from collections import deque
from dataclasses import dataclass, replace
from datetime import date
from typing import Iterator, List, Deque

from .client import InfluxHandle


@dataclass(frozen=True)
class DrawdownData:
    day: date
    current_drawdown: float
    days_since_dip: int
    fwd_max_drawdown: float


def _flux(bucket: str) -> str:
    return f'''
from(bucket: "{bucket}")
  |> range(start: time(v: "2016-02-01"), stop: now())
  |> filter(fn: (r) => r._measurement == "drawdown-analysis")
  |> filter(fn: (r) => r._field == "drawdownAvg")
  |> keep(columns: ["_time", "_value"])
'''.strip()


def _add_fwd_max_drawdown(rows: list[DrawdownData], future_window_days: int) -> List[DrawdownData]:
    rows = sorted(rows, key=lambda r: r.day)
    levels = [r.current_drawdown for r in rows]
    row_count = len(rows)
    dq: Deque[int] = deque()

    def push(idx: int):
        while dq and levels[dq[-1]] >= levels[idx]:
            dq.pop()
        dq.append(idx)

    # prefill indices 1..n
    for idx in range(1, min(row_count, future_window_days + 1)):
        push(idx)

    out = []
    for i in range(row_count - future_window_days):
        while dq and dq[0] < i + 1:
            dq.popleft()

        out.append(replace(rows[i], fwd_max_drawdown=levels[dq[0]]))

        nxt = i + future_window_days + 1
        if nxt < row_count:
            push(nxt)

    return out


def _add_days_since_dip(rows: list[DrawdownData]) -> list[DrawdownData]:
    rows = sorted(rows, key=lambda r: r.day)
    out: list[DrawdownData] = []
    days_since_dip = 0

    for row in rows:
        days_since_dip = 0 if row.current_drawdown <= 95 else days_since_dip + 1
        out.append(replace(row, days_since_dip=days_since_dip))

    return out


def drawdown_query(h: InfluxHandle, fwd_bank_days: int) -> Iterator[DrawdownData]:
    logger = logging.getLogger(__name__)

    fetch = h.query_api.query(_flux(h.bucket))
    logger.info(f"Fetched drawdown data from InfluxDB, got {len(fetch)} tables")

    out: List[DrawdownData] = []
    for table in fetch:
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

    yield from out
