import logging
from dataclasses import dataclass
from datetime import date
from typing import Iterable

from narwhal.domain.constants import (
    BANK_DAYS_OF_TWO_WEEKS,
    BANK_DAYS_OF_TWO_MONTHS,
    BANK_DAYS_OF_FOUR_MONTHS,
    BANK_DAYS_OF_FIVE_WEEKS,
)
from narwhal.domain.schema.forward_max_drawdown.day_bundle import ForwardMaxDrawdownDays
from narwhal.domain.schema.forward_max_drawdown.training_fields import ForwardMaxDrawdownFields
from narwhal.domain.schema.prediction_result import ForwardMaxDrawdownPredictionResult
from narwhal.services.runners.runner_base import RunnerBase
from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.market_data.drawdown import drawdown_query
from narwhal.sources.influx.market_data.index_data import index_query
from narwhal.sources.influx.market_data.members import member_query
from narwhal.sources.influx.market_data.vix import vix_query
from narwhal.sources.influx.market_data.volume import volume_query
from narwhal.sources.influx.training_data.drawdown import query_forward_max_drawdown

logger = logging.getLogger(__name__)


@dataclass(frozen=True)
class ForwardMaxDrawdownRunner(
    RunnerBase[ForwardMaxDrawdownDays, ForwardMaxDrawdownFields, ForwardMaxDrawdownPredictionResult]
):
    drawdown_days: int
    index_days: int
    program_name: str
    volume_days: int
    variant: str

    def bundle(self, h: InfluxHandle) -> Iterable[ForwardMaxDrawdownDays]:
        return ForwardMaxDrawdownDays.daily_generator(
            drawdown=drawdown_query(h, self.drawdown_days),
            index=index_query(h, self.index_days),
            members=member_query(h),
            vix=vix_query(h),
            volume=volume_query(h, self.volume_days),
        )

    def compose(self, bundles: Iterable[ForwardMaxDrawdownDays]) -> list[ForwardMaxDrawdownFields]:
        return [ForwardMaxDrawdownFields.compose(bundle=b, variant=self.variant) for b in bundles]

    def map_predictions(
        self, predictions: Iterable[tuple[date, float]]
    ) -> list[ForwardMaxDrawdownPredictionResult]:
        return [
            ForwardMaxDrawdownPredictionResult(variant=self.variant, timestamp=ts, field_value=v)
            for (ts, v) in predictions
        ]

    def query(self, h: InfluxHandle) -> Iterable[ForwardMaxDrawdownFields]:
        return query_forward_max_drawdown(h, self.variant)


FORWARD_MAX_DRAWDOWN_RUNS: set[ForwardMaxDrawdownRunner] = {
    ForwardMaxDrawdownRunner(
        program_name="forward-max-drawdown-two-weeks",
        variant="two-weeks",
        drawdown_days=BANK_DAYS_OF_TWO_WEEKS,
        index_days=BANK_DAYS_OF_TWO_MONTHS,
        volume_days=BANK_DAYS_OF_TWO_MONTHS,
    ),
    ForwardMaxDrawdownRunner(
        program_name="forward-max-drawdown-five-weeks",
        variant="five-weeks",
        drawdown_days=BANK_DAYS_OF_FIVE_WEEKS,
        index_days=BANK_DAYS_OF_FOUR_MONTHS,
        volume_days=BANK_DAYS_OF_FOUR_MONTHS,
    ),
}
