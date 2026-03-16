from dataclasses import dataclass
from datetime import date
from typing import ClassVar

from narwhal.sources.influx.helpers import to_epoch_ns


@dataclass(frozen=True)
class PredictionResultBase:
    COLLECTION: ClassVar[str]
    timestamp: date
    variant: str

    field_name: ClassVar[str]
    field_value: float

    def to_line_protocol(self) -> str:
        measurement = f"{self.COLLECTION}-{self.variant}" if self.variant else self.COLLECTION
        return f"{measurement} {self.field_name}={self.field_value} {to_epoch_ns(self.timestamp)}"


@dataclass(frozen=True)
class DrawdownNextBankDayPredictionResult(PredictionResultBase):
    COLLECTION = "drawdown-next-bank-day-prediction-results"
    field_name = "drawdown_next_bank_day"


@dataclass(frozen=True)
class ForwardMaxDrawdownPredictionResult(PredictionResultBase):
    COLLECTION = "forward-max-drawdown-prediction-results"
    field_name = "forward_max_drawdown"
