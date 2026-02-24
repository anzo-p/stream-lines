from dataclasses import dataclass, fields
from datetime import date
from typing import ClassVar, Sequence, Any

import numpy as np

from narwhal.sources.influx.helpers import to_epoch_ns


@dataclass(frozen=True)
class TrainingData:
    FEATURES: ClassVar[Sequence[str]] = (
        "members_daily_spread",
        "index_over_moving_avg",
        "index_over_kaufman_avg",
        "volume_over_moving_avg",
        "current_drawdown",
        "days_since_dip",
        "vix",
    )

    TRAINING_FIELDS: ClassVar[Sequence[str]] = (
        "fwd_max_drawdown",
        *FEATURES,
    )

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

    def __init_subclass__(cls, **kwargs: dict[str, Any]) -> None:
        super().__init_subclass__(**kwargs)

        field_names = {f.name for f in fields(cls)}
        expected = set(cls.TRAINING_FIELDS) | {"timestamp", "measurement"}

        missing = set(cls.TRAINING_FIELDS) - field_names
        if missing:
            raise TypeError(f"{cls.__name__}.TRAINING_FIELDS has unknown fields: {sorted(missing)}")

        extras = field_names - expected
        if extras:
            raise TypeError(f"{cls.__name__} has fields not in TRAINING_FIELDS: {sorted(extras)}")

    def to_line_protocol(self) -> str:
        fields = []
        for name in type(self).TRAINING_FIELDS:
            value = getattr(self, name)
            if isinstance(value, int):
                fields.append(f"{name}={value}i")
            else:
                fields.append(f"{name}={value}")

        return f"{self.measurement} {','.join(fields)} {to_epoch_ns(self.timestamp)}"

    def x_vector(self) -> np.ndarray:
        return np.array([getattr(self, name) for name in self.FEATURES], dtype=float)
