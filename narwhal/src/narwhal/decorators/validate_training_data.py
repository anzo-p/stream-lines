from dataclasses import fields
from typing import Any, cast, ClassVar, Protocol, TypeVar, Callable

from narwhal.domain.schema.helpers import check_schema


class TrainingFieldsType(Protocol):
    LABEL: ClassVar[str]
    FIELD_EXTRACTORS: ClassVar[dict[str, Callable[[Any], Any]]]


T = TypeVar("T", bound=TrainingFieldsType)


def validate_training_fields(cls: type[T]) -> type[T]:

    # is_dataclass(cls) doesn't work with Protocols
    if not hasattr(cls, "__dataclass_fields__"):
        raise TypeError(f"{cls.__name__} must be a dataclass")

    check_schema(
        entity=f"{cls.__name__}.FIELD_EXTRACTORS",
        expected=set(cls.FIELD_EXTRACTORS),
        actual={f.name for f in fields(cast(Any, cls))} - {"variant"},
    )

    first = next(k for k in cls.FIELD_EXTRACTORS if k != "timestamp")
    assert first == cls.LABEL, (
        f"{cls.__name__}.FIELD_EXTRACTORS must begin with the LABEL: {cls.LABEL}, got {first}"
    )

    return cls
