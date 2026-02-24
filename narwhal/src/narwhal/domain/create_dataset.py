import logging
from dataclasses import fields
from datetime import date
from typing import Any, Callable, ParamSpec, TypeVar, Iterable

from narwhal.domain.constants import BANK_DAYS_OF_TWO_MONTHS, BANK_DAYS_OF_TWO_WEEKS
from narwhal.domain.day_bundle import DayBundle
from narwhal.domain.training_data import TrainingData
from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.drawdown import drawdown_query
from narwhal.sources.influx.index_data import index_query
from narwhal.sources.influx.members import member_query
from narwhal.sources.influx.query_result import QueryResult
from narwhal.sources.influx.vix import vix_query
from narwhal.sources.influx.volume import volume_query

P = ParamSpec("P")
R = TypeVar("R", bound=QueryResult)


logger = logging.getLogger(__name__)


FIELD_EXTRACTORS: dict[str, Callable[[DayBundle], Any]] = {
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
_expected = {f.name for f in fields(TrainingData)}
_actual = set(FIELD_EXTRACTORS)
if _expected != _actual:
    raise TypeError(
        f"Extractor schema mismatch. "
        f"missing={sorted(_expected - _actual)}, "
        f"extra={sorted(_actual - _expected)}"
    )


def _compose_row(bundle: DayBundle) -> TrainingData:
    kwargs = {name: fn(bundle) for name, fn in FIELD_EXTRACTORS.items()}
    return TrainingData(**kwargs)


def _index_by_day(rows: Iterable[R]) -> dict[date, R]:
    return {r.day: r for r in rows}


def compose_training_data(h: InfluxHandle) -> list[TrainingData]:
    drawdown_by_day = _index_by_day(drawdown_query(h, BANK_DAYS_OF_TWO_WEEKS))
    index_by_day = _index_by_day(index_query(h, BANK_DAYS_OF_TWO_MONTHS))
    members_by_day = _index_by_day(member_query(h))
    vix_by_day = _index_by_day(vix_query(h))
    volume_by_day = _index_by_day(volume_query(h, BANK_DAYS_OF_TWO_MONTHS))

    common_days = (
        members_by_day.keys()
        & index_by_day.keys()
        & volume_by_day.keys()
        & drawdown_by_day.keys()
        & vix_by_day.keys()
    )

    out: list[TrainingData] = [
        _compose_row(
            DayBundle(
                day=day,
                drawdown=drawdown_by_day[day],
                index=index_by_day[day],
                members=members_by_day[day],
                volume=volume_by_day[day],
                vix=vix_by_day[day],
            )
        )
        for day in sorted(common_days)
    ]

    logger.debug("Combined members (first 5): %s", out[:5])
    return out
