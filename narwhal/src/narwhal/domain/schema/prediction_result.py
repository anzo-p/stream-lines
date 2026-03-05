from dataclasses import dataclass
from datetime import date
from typing import ClassVar

from narwhal.sources.influx.helpers import to_epoch_ns


@dataclass(frozen=True)
class PredictionResultBase:
    COLLECTION: ClassVar[str]
    timestamp: date
    variant: str

    @property
    def fields(self) -> str:
        raise NotImplementedError

    def to_line_protocol(self) -> str:
        measurement = f"{self.COLLECTION}-{self.variant}" if self.variant else self.COLLECTION
        return f"{measurement} {self.fields} {to_epoch_ns(self.timestamp)}"


@dataclass(frozen=True)
class DrawdownPredictionResult(PredictionResultBase):
    COLLECTION = "drawdown-prediction-results"
    fwd_max_drawdown: float

    @property
    def fields(self) -> str:
        return f"fwd_max_drawdown={self.fwd_max_drawdown}"
