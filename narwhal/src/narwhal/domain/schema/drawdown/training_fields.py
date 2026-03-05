from dataclasses import dataclass
from typing import ClassVar, Callable, Any, Self

from narwhal.decorators.validate_training_data import validate_training_fields
from narwhal.domain.schema.drawdown.day_bundle import DrawdownDayBundle
from narwhal.domain.schema.training_fields_base import TrainingFieldsBase


@validate_training_fields
@dataclass(frozen=True)
class DrawdownTrainingFields(TrainingFieldsBase):
    LABEL = "fwd_max_drawdown"
    COLLECTION = "drawdown-training-data"

    fwd_max_drawdown: float
    members_daily_spread: float
    index_over_moving_avg: float
    index_over_kaufman_avg: float
    volume_over_moving_avg: float
    current_drawdown: float
    days_since_dip_of_3: int
    days_since_dip_of_5: int
    days_since_dip_of_8: int
    days_since_dip_of_13: int
    vix: float

    FIELD_EXTRACTORS: ClassVar[dict[str, Callable[[DrawdownDayBundle], Any]]] = (
        TrainingFieldsBase.FIELD_EXTRACTORS
        | {
            "fwd_max_drawdown": lambda d: d.drawdown.fwd_max_drawdown,
            "members_daily_spread": lambda d: d.members.daily_spread,
            "index_over_moving_avg": lambda d: d.index.over_moving_avg,
            "index_over_kaufman_avg": lambda d: d.index.over_kaufman_avg,
            "volume_over_moving_avg": lambda d: d.volume.over_moving_avg,
            "current_drawdown": lambda d: d.drawdown.current_drawdown,
            "days_since_dip_of_3": lambda d: d.drawdown.days_since_dip_of_3,
            "days_since_dip_of_5": lambda d: d.drawdown.days_since_dip_of_5,
            "days_since_dip_of_8": lambda d: d.drawdown.days_since_dip_of_8,
            "days_since_dip_of_13": lambda d: d.drawdown.days_since_dip_of_13,
            "vix": lambda d: d.vix.value,
        }
    )

    @classmethod
    def compose(cls, bundle: DrawdownDayBundle, variant: str) -> Self:
        return super().compose(bundle, variant)
