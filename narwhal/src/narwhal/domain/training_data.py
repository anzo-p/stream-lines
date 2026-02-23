from dataclasses import dataclass
from datetime import date
from typing import ClassVar

from narwhal.sources.influx.helpers import to_epoch_ns


@dataclass(frozen=True)
class TrainingData:
    measurement: ClassVar[str] = "drawdown-training-data"

    timestamp: date
    fwd_max_drawdown: float
    members_daily_spread: float
    index_over_moving_avg: float
    index_over_kaufman_avg: float
    volume_over_moving_avg: float
    current_drawdown: float
    days_since_dip: int
    vix: float

    def to_line_protocol(self) -> str:
        field_str = (
            f"fwd_max_drawdown={self.fwd_max_drawdown},"
            f"members_daily_spread={self.members_daily_spread},"
            f"index_over_moving_avg={self.index_over_moving_avg},"
            f"index_over_kaufman_avg={self.index_over_kaufman_avg},"
            f"volume_over_moving_avg={self.volume_over_moving_avg},"
            f"current_drawdown={self.current_drawdown},"
            f"days_since_dip={self.days_since_dip}i,"
            f"vix={self.vix}"
        )
        return f"{self.measurement} {field_str} {to_epoch_ns(self.timestamp)}"
