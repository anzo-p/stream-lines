from dataclasses import dataclass, fields
from datetime import date
from typing import Any, cast, Iterable, Mapping, Self, TypeAlias

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
    def _feature_fields(cls) -> tuple[str, ...]:
        return tuple(f.name for f in fields(cast(Any, cls)) if f.name != "day")

    @classmethod
    def daily_generator(cls: type[Self], **rows_by_name: Iterable[QueryResult]) -> Iterable[Self]:
        names = cls._feature_fields()

        check_schema(
            entity=f"{cls.__name__} fields",
            expected=set(names),
            actual=set(rows_by_name.keys()),
        )

        indexed = {n: _index_by_day(rows_by_name[n]) for n in names}

        for day in _common_dates(indexed, names):
            data = {n: indexed[n][day] for n in names}
            ctor = cast(Any, cls)  # satisfy Mypy
            yield cast(Self, ctor(day=day, **data))


def _common_dates(indexed: Mapping[str, Mapping[date, Any]], names: tuple[str, ...]) -> list[date]:
    dicts = [indexed[n] for n in names]
    out = set.intersection(*(set(d.keys()) for d in dicts)) if dicts else set()
    return sorted(out)


def _index_by_day(rows: Iterable[QueryResult]) -> dict[date, QueryResult]:
    return {r.day: r for r in rows}
