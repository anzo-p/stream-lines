import logging
from datetime import date
from itertools import islice
from typing import Iterable, Callable, TypeVar, List

from narwhal.domain.constants import BANK_DAYS_OF_TWO_MONTHS, BANK_DAYS_OF_TWO_WEEKS
from narwhal.domain.training_data import TrainingData
from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.drawdown import DrawdownData, drawdown_query
from narwhal.sources.influx.index_data import IndexData, index_query
from narwhal.sources.influx.members import MemberData, member_query
from narwhal.sources.influx.serializable import Serializable
from narwhal.sources.influx.training_data import training_data_query
from narwhal.sources.influx.volume import VolumeData, volume_query

T = TypeVar("T")


logger = logging.getLogger(__name__)


def _batched(iterable: Iterable[T], size: int) -> Iterable[list[T]]:
    it = iter(iterable)
    while batch := list(islice(it, size)):
        yield batch


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
                timestamp=day,
                fwd_max_drawdown=drawdown_by_day[day].fwd_max_drawdown,
                members_daily_spread=members_by_day[day].daily_spread,
                index_over_moving_avg=index_data_by_day[day].over_moving_avg,
                index_over_kaufman_avg=index_data_by_day[day].over_kaufman_avg,
                volume_over_moving_avg=volume_by_day[day].over_moving_avg,
                current_drawdown=drawdown_by_day[day].current_drawdown,
                days_since_dip=drawdown_by_day[day].days_since_dip,
            )
        )

    logger.debug(f"Combined members: {out}")

    return out


def _index_by_day(rows: Iterable[T], get_day: Callable[[T], date]) -> dict[date, T]:
    return {get_day(r): r for r in rows}


def _submit_to_influx(handle, data, batch_size=10):
    for batch in _batched(data, batch_size):
        try:
            payload = "\n".join(d.to_line_protocol() for d in batch)
            handle.write_api.write(
                bucket=handle.bucket,
                org=handle.org,
                record=payload,
            )
        except Exception:
            logger.exception("Failed writing batch to InfluxDB", extra={"batch_size": len(batch)})


def fetch_training_data(h: InfluxHandle) -> list[TrainingData]:
    return list(training_data_query(h))


def fetch_raw_data(h: InfluxHandle) -> list[TrainingData]:
    drawdown: List[DrawdownData] = list(drawdown_query(h, BANK_DAYS_OF_TWO_WEEKS))
    index: List[IndexData] = list(index_query(h, BANK_DAYS_OF_TWO_MONTHS))
    members: List[MemberData] = list(member_query(h))
    volume: List[VolumeData] = list(volume_query(h, BANK_DAYS_OF_TWO_MONTHS))

    logger.debug(
        f"Fetched members: {members}, index: {index}, volume: {volume}, drawdown: {drawdown}"
    )

    return _combine_by_day(members, index, volume, drawdown)


def write_to_influx(data: Iterable[Serializable], handle: InfluxHandle) -> None:
    _submit_to_influx(handle, data)
