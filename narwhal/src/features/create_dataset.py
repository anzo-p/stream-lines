from datetime import date
from typing import Iterable, Callable, TypeVar, List

from src.config import BANK_DAYS_OF_TWO_MONTHS, BANK_DAYS_OF_TWO_WEEKS
from src.features.schema import TrainingData
from src.sources.influxdb.client import InfluxHandle
from src.sources.influxdb.drawdown import DrawdownData, drawdown_query
from src.sources.influxdb.index_data import IndexData, index_query
from src.sources.influxdb.members import MemberData, member_query
from src.sources.influxdb.volume import VolumeData, volume_query

T = TypeVar("T")


def _index_by_day(rows: Iterable[T], get_day: Callable[[T], date]) -> dict[date, T]:
    return {get_day(r): r for r in rows}


def _combine_by_day(
    members: Iterable[MemberData],
    index_data: Iterable[IndexData],
    volumes: Iterable[VolumeData],
    drawdowns: Iterable[DrawdownData],
) -> list[TrainingData]:
    members_by_day = _index_by_day(members, lambda r: r.day)
    index_data_by_day = _index_by_day(index_data, lambda r: r.day)
    volume_by_day = _index_by_day(volumes, lambda r: r.day)
    drawdown_by_day = _index_by_day(drawdowns, lambda r: r.day)

    common_days = set.intersection(
        *(
            set(d.keys())
            for d in (members_by_day, index_data_by_day, volume_by_day, drawdown_by_day)
        )
    )

    out: list[TrainingData] = []
    for day in sorted(common_days):
        out.append(
            TrainingData(
                fwd_max_drawdown=drawdown_by_day[day].fwd_max_drawdown,
                members_daily_spread=members_by_day[day].daily_spread,
                index_over_moving_avg=index_data_by_day[day].over_moving_avg,
                index_over_kaufman_avg=index_data_by_day[day].over_kaufman_avg,
                volume_over_moving_avg=volume_by_day[day].over_moving_avg,
                current_drawdown=drawdown_by_day[day].current_drawdown,
                days_since_dip=drawdown_by_day[day].days_since_dip,
            )
        )

    return out


def fetch_and_compose(historical_data: InfluxHandle) -> list[TrainingData]:
    members: List[MemberData] = list(member_query(historical_data))
    index: List[IndexData] = list(index_query(historical_data, BANK_DAYS_OF_TWO_MONTHS))
    volume: List[VolumeData] = list(volume_query(historical_data, BANK_DAYS_OF_TWO_MONTHS))
    drawdown: List[DrawdownData] = list(drawdown_query(historical_data, BANK_DAYS_OF_TWO_WEEKS))

    return _combine_by_day(members, index, volume, drawdown)
