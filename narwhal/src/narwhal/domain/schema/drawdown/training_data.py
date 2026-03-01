from dataclasses import dataclass
from datetime import date
from typing import ClassVar, Sequence, Callable, Any, Self

from narwhal.decorators.validate_training_schema import validate_training_schema
from narwhal.domain.schema.training_data_base import TrainingDataBase
from narwhal.domain.schema.drawdown.day_bundle import DrawdownDayBundle


@validate_training_schema
@dataclass(frozen=True)
class DrawdownTrainingData(TrainingDataBase):
    LABEL = "fwd_max_drawdown"

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
        LABEL,
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

    FIELD_EXTRACTORS: ClassVar[dict[str, Callable[[DrawdownDayBundle], Any]]] = (
        TrainingDataBase.FIELD_EXTRACTORS
        | {
            "fwd_max_drawdown": lambda d: d.drawdown.fwd_max_drawdown,
            "members_daily_spread": lambda d: d.members.daily_spread,
            "index_over_moving_avg": lambda d: d.index.over_moving_avg,
            "index_over_kaufman_avg": lambda d: d.index.over_kaufman_avg,
            "volume_over_moving_avg": lambda d: d.volume.over_moving_avg,
            "current_drawdown": lambda d: d.drawdown.current_drawdown,
            "days_since_dip": lambda d: d.drawdown.days_since_dip,
            "vix": lambda d: d.vix.value,
        }
    )

    @classmethod
    def compose_row(cls, bundle: DrawdownDayBundle) -> Self:
        kwargs = {name: fn(bundle) for name, fn in cls.FIELD_EXTRACTORS.items()}
        return cls(**kwargs)
