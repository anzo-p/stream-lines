from dataclasses import dataclass
from datetime import date
from typing import Any, Callable, ClassVar, Iterable, Self

import numpy as np

from narwhal.sources.influx.helpers import to_epoch_ns


@dataclass(frozen=True)
class TrainingFieldsBase:
    """
    TrainingData defines features to train and predict the label

    Main concepts
    -------------
    - Provides a way to take a DayBundle of source data to TrainingData
    - Helpers to applicable libraries and outputs
    """

    LABEL: ClassVar[str]
    FIELD_EXTRACTORS: ClassVar[dict[str, Callable[[Any], Any]]] = {
        "timestamp": lambda d: d.day,
    }
    COLLECTION: ClassVar[str]

    timestamp: date
    variant: str

    def _dataset_names(self) -> Iterable[str]:
        return self._names(excluded={"timestamp"})

    def _feature_names(self) -> Iterable[str]:
        return self._names(excluded={type(self).LABEL, "timestamp"})

    def _names(self, excluded: set[str]) -> Iterable[str]:
        names: Iterable[str] = type(self).FIELD_EXTRACTORS.keys()
        return (n for n in names if n not in excluded)

    @classmethod
    def compose(cls, bundle: Any, variant: str) -> Self:
        kwargs = {name: fn(bundle) for name, fn in cls.FIELD_EXTRACTORS.items()}
        return cls(**kwargs, variant=variant)

    def to_feature_set(self) -> list[object]:
        return [getattr(self, c) for c in self._dataset_names()]

    def to_line_protocol(self) -> str:
        fields = []
        for name in self._dataset_names():
            value = getattr(self, name)
            if isinstance(value, int):
                fields.append(f"{name}={value}i")
            else:
                fields.append(f"{name}={value}")

        measurement = f"{self.COLLECTION}-{self.variant}"
        field_values = ",".join(fields)
        ts = to_epoch_ns(self.timestamp)
        return f"{measurement} {field_values} {ts}"

    def x_vector(self) -> np.ndarray:
        X = np.array([getattr(self, name) for name in self._feature_names()], dtype=float)
        return X.reshape(1, -1)
