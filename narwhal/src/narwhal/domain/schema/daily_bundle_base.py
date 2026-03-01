from dataclasses import dataclass, fields
from datetime import date
from typing import Mapping, Any, cast, Self, Iterable, TypeAlias

from narwhal.domain.schema.helpers import check_schema
from narwhal.sources.influx.query_result import QueryResult


IndexedByDay: TypeAlias = dict[str, dict[date, Any]]


@dataclass(frozen=True)
class DayBundleBase:
    """
    DayBundle groups and sorts datasets by day

    Main concepts
    -------------
    Requires subclasses to contain only fields of type QueryResult
    QueryResult in turn requires the grouping field day: date
    Provides a generator api to take a set[Iterable[QueryResult]] into Iterable[DayBundle]
    """

    day: date

    @classmethod
    def _common_dates(cls, entities_by_day: Mapping[str, Mapping[date, Any]]) -> list[date]:
        dicts = [entities_by_day[name] for name in cls._feature_fields()]
        out = set.intersection(*(set(d.keys()) for d in dicts)) if dicts else set()
        return sorted(out)

    @classmethod
    def _feature_fields(cls) -> tuple[str, ...]:
        return tuple(f.name for f in fields(cast(Any, cls)) if f.name != "day")

    @classmethod
    def _from_indexed(
        cls: type[Self], day: date, indexed: Mapping[str, Mapping[date, Any]]
    ) -> Self:
        data = {name: indexed[name][day] for name in cls._feature_fields()}
        ctor = cast(Any, cls)  # satisfy Mypy
        return cast(Self, ctor(day=day, **data))

    @classmethod
    def _index_all_by_day(cls, **rows_by_name: Iterable[QueryResult]) -> IndexedByDay:
        check_schema(
            entity=f"{cls.__name__} fields",
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
