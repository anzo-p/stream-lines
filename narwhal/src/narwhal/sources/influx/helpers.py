from datetime import datetime, timezone
from typing import TypeVar

from narwhal.domain.constants import ALPACA_DATA_DAWN

T = TypeVar("T")


def compose_default_range() -> str:
    return f'range(start: time(v: "{ALPACA_DATA_DAWN.isoformat()}"), stop: now())'


def to_epoch_ns(d):
    dt = datetime(d.year, d.month, d.day, tzinfo=timezone.utc)
    return int(dt.timestamp() * 1_000_000_000)
