from dataclasses import dataclass
from datetime import date
from typing import Iterable

from narwhal.domain.constants import BANK_DAYS_OF_TWO_MONTHS
from narwhal.domain.schema.drawdown_next_bank_day.day_bundle import DrawdownNextBankDayDays
from narwhal.domain.schema.drawdown_next_bank_day.training_fields import DrawdownNextBankDayFields
from narwhal.domain.schema.prediction_result import DrawdownNextBankDayPredictionResult
from narwhal.services.runners.runner_base import RunnerBase
from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.market_data.drawdown import drawdown_query
from narwhal.sources.influx.market_data.index_data import index_query
from narwhal.sources.influx.market_data.members import member_query
from narwhal.sources.influx.market_data.vix import vix_query
from narwhal.sources.influx.market_data.volume import volume_query
from narwhal.sources.influx.training_data.drawdown import query_drawdown_next_bank_day


@dataclass(frozen=True)
class DrawdownNextBankDayRunner(
    RunnerBase[
        DrawdownNextBankDayDays, DrawdownNextBankDayFields, DrawdownNextBankDayPredictionResult
    ]
):
    index_days: int
    program_name: str
    volume_days: int

    drawdown_days: int = 0
    variant: str = ""

    def bundle(self, h: InfluxHandle) -> Iterable[DrawdownNextBankDayDays]:
        return DrawdownNextBankDayDays.daily_generator(
            drawdown=drawdown_query(h, self.drawdown_days),
            index=index_query(h, self.index_days),
            members=member_query(h),
            vix=vix_query(h),
            volume=volume_query(h, self.volume_days),
        )

    def compose(
        self, bundles: Iterable[DrawdownNextBankDayDays]
    ) -> list[DrawdownNextBankDayFields]:
        return [DrawdownNextBankDayFields.compose(bundle=b, variant=self.variant) for b in bundles]

    def map_predictions(
        self, predictions: Iterable[tuple[date, float]]
    ) -> list[DrawdownNextBankDayPredictionResult]:
        return [
            DrawdownNextBankDayPredictionResult(variant=self.variant, timestamp=ts, field_value=v)
            for (ts, v) in predictions
        ]

    def query(self, h: InfluxHandle) -> Iterable[DrawdownNextBankDayFields]:
        return query_drawdown_next_bank_day(h)


DRAWDOWN_NEXT_BANK_DAY_RUNS: set[DrawdownNextBankDayRunner] = {
    DrawdownNextBankDayRunner(
        program_name="drawdown-next-bank-day",
        index_days=BANK_DAYS_OF_TWO_MONTHS,
        volume_days=BANK_DAYS_OF_TWO_MONTHS,
    )
}
