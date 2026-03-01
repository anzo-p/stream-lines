from datetime import date
from typing import ClassVar, Sequence, Callable, Any, Self

import numpy as np

from narwhal.sources.influx.helpers import to_epoch_ns


class TrainingDataBase:
    """
    TrainingData defines a feature schema to train and predict its label

    Main concepts
    -------------
    - Will validate that everything adheres to that feature schema
    - Provides a way to take DayBundle to TrainingData with defined field extractors
    - Provides mappers to applicable libraries, e.g. databases and numpy

    Short guide
    -----------
    - Always start by editing subclass TRAINING_FIELDS
    - Each field will require an extractor
    - Those will require respective QueryResult fields in associated DayBundle
    """

    LABEL: str
    FEATURES: ClassVar[Sequence[str]]
    TRAINING_FIELDS: ClassVar[Sequence[str]]
    FIELD_EXTRACTORS: ClassVar[dict[str, Callable[[Any], Any]]] = {
        "timestamp": lambda d: d.day,
    }

    measurement: ClassVar[str]
    timestamp: date

    @classmethod
    def compose_row(cls, bundle: Any) -> Self:
        raise NotImplementedError

    def to_line_protocol(self) -> str:
        fields = []
        for name in type(self).TRAINING_FIELDS:
            value = getattr(self, name)
            if isinstance(value, int):
                fields.append(f"{name}={value}i")
            else:
                fields.append(f"{name}={value}")

        return f"{self.measurement} {','.join(fields)} {to_epoch_ns(self.timestamp)}"

    def x_vector(self) -> np.ndarray:
        X = np.array([getattr(self, name) for name in self.FEATURES], dtype=float)
        return X.reshape(1, -1)
