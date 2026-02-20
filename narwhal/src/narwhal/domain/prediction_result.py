from dataclasses import dataclass
from datetime import date
from typing import ClassVar

from narwhal.sources.influx.helpers import to_epoch_ns


@dataclass(frozen=True)
class PredictionResult:
    measurement: ClassVar[str] = "drawdown-prediction-result"

    timestamp: date
    fwd_max_drawdown: float

    def to_line_protocol(self) -> str:
        field_str = f"fwd_max_drawdown={self.fwd_max_drawdown}"

        return f"{self.measurement} {field_str} {to_epoch_ns(self.timestamp)}"
