from dataclasses import fields
from typing import Any, Callable

from narwhal.domain.schema.day_bundle import DayBundleSchema
from narwhal.domain.schema.helpers import check_schema
from narwhal.domain.schema.training_data import TrainingData

FIELD_EXTRACTORS: dict[str, Callable[[DayBundleSchema], Any]] = {
    "timestamp": lambda d: d.day,
    "fwd_max_drawdown": lambda d: d.drawdown.fwd_max_drawdown,
    "members_daily_spread": lambda d: d.members.daily_spread,
    "index_over_moving_avg": lambda d: d.index.over_moving_avg,
    "index_over_kaufman_avg": lambda d: d.index.over_kaufman_avg,
    "volume_over_moving_avg": lambda d: d.volume.over_moving_avg,
    "current_drawdown": lambda d: d.drawdown.current_drawdown,
    "days_since_dip": lambda d: d.drawdown.days_since_dip,
    "vix": lambda d: d.vix.value,
}

check_schema(
    entity="FIELD_EXTRACTORS",
    expected=set({f.name for f in fields(TrainingData)}),
    actual=set(FIELD_EXTRACTORS),
)
