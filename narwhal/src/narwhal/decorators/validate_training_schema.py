from dataclasses import fields
from typing import Any, cast, ClassVar, Protocol, Sequence, TypeVar, Callable

from narwhal.domain.schema.helpers import check_schema


class TrainingDataType(Protocol):
    TRAINING_FIELDS: ClassVar[Sequence[str]]
    FIELD_EXTRACTORS: ClassVar[dict[str, Callable[[Any], Any]]]


T = TypeVar("T", bound=TrainingDataType)


def validate_training_schema(cls: type[T]) -> type[T]:

    # is_dataclass(cls) doesn't work with Protocols
    if not hasattr(cls, "__dataclass_fields__"):
        raise TypeError(f"{cls.__name__} must be a dataclass")

    actual = {f.name for f in fields(cast(Any, cls))}

    check_schema(
        entity=f"{cls.__name__}.TRAINING_FIELDS",
        expected=set(cls.TRAINING_FIELDS) | {"timestamp"},
        actual=actual,
    )
    check_schema(
        entity=f"{cls.__name__}.FIELD_EXTRACTORS", expected=set(cls.FIELD_EXTRACTORS), actual=actual
    )

    return cls
