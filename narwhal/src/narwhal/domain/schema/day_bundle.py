from dataclasses import dataclass, fields
from datetime import date
from typing import Any, Iterable, Mapping, TypeAlias, Self

from narwhal.domain.schema.helpers import check_schema
from narwhal.sources.influx.drawdown import DrawdownData
from narwhal.sources.influx.index_data import IndexData
from narwhal.sources.influx.members import MemberData
from narwhal.sources.influx.query_result import QueryResult
from narwhal.sources.influx.vix import VixData
from narwhal.sources.influx.volume import VolumeData

IndexedByDay: TypeAlias = dict[str, dict[date, Any]]


@dataclass(frozen=True)
class DayBundleSchema:
    day: date

    # manage QueryResult data sources here to group them by day
    drawdown: DrawdownData
    index: IndexData
    members: MemberData
    vix: VixData
    volume: VolumeData

    @classmethod
    def _common_dates(cls, entities_by_day: Mapping[str, Mapping[date, Any]]) -> list[date]:
        dicts = [entities_by_day[name] for name in cls._feature_fields()]
        out = set.intersection(*(set(d.keys()) for d in dicts)) if dicts else set()
        return sorted(out)

    @classmethod
    def _feature_fields(cls) -> tuple[str, ...]:
        return tuple(f.name for f in fields(cls) if f.name != "day")

    @classmethod
    def _from_indexed(
        cls: type[Self], day: date, indexed: Mapping[str, Mapping[date, Any]]
    ) -> Self:
        data = {name: indexed[name][day] for name in cls._feature_fields()}
        return cls(day=day, **data)

    @classmethod
    def _index_all_by_day(cls, **rows_by_name: Iterable[QueryResult]) -> IndexedByDay:
        check_schema(
            entity="DayBundleSchema fields",
            expected=set(cls._feature_fields()),
            actual=set(rows_by_name.keys()),
        )
        return {name: cls._index_by_day(rows_by_name[name]) for name in cls._feature_fields()}

    @staticmethod
    def _index_by_day(rows: Iterable[QueryResult]) -> dict[date, QueryResult]:
        return {r.day: r for r in rows}

    @classmethod
    def daily_generator(cls: type[Self], **rows_by_name: Iterable[QueryResult]) -> Iterable[Self]:
        indexed_by_day = cls._index_all_by_day(**rows_by_name)
        for day in cls._common_dates(indexed_by_day):
            yield cls._from_indexed(day, indexed_by_day)
