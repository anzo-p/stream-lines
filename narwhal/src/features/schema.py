from dataclasses import dataclass


@dataclass(frozen=True)
class TrainingData:
    fwd_max_drawdown: float
    members_daily_spread: float
    index_over_moving_avg: float
    index_over_kaufman_avg: float
    volume_over_moving_avg: float
    current_drawdown: float
    days_since_dip: int
