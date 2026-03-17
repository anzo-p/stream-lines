import logging
from dataclasses import dataclass
from datetime import date
from typing import Generic, Iterable, TypeVar, List

import numpy as np
import xgboost
from xgboost import Booster, DMatrix

from narwhal.domain.schema.training_fields_base import TrainingFieldsBase
from narwhal.sinks.influx.write import write_to_influx
from narwhal.sinks.s3.export_file import export_to_file
from narwhal.sources.influx.client import (
    InfluxHandle,
)
from narwhal.sources.influx.serializable import Serializable

logger = logging.getLogger(__name__)


T = TypeVar("T")
U = TypeVar("U", bound="TrainingFieldsBase")
V = TypeVar("V", bound="Serializable")


@dataclass(frozen=True)
class RunnerBase(Generic[T, U, V]):
    program_name: str

    def bundles(self, h: InfluxHandle) -> Iterable[T]:
        raise NotImplementedError

    def compose(self, bundles: Iterable[T]) -> list[U]:
        raise NotImplementedError

    def map_predictions(self, predictions: Iterable[tuple[date, float]]) -> list[V]:
        raise NotImplementedError

    def query(self, h: InfluxHandle) -> Iterable[U]:
        raise NotImplementedError

    def _predict_batch(self, data: list[U], model: Booster) -> Iterable[tuple[date, float]]:
        if not data:
            return

        X_batch = np.vstack([e.x_vector() for e in data])
        logger.debug(f"XGBoost Batch DMatrix shape: {X_batch.shape}")

        predictions = model.predict(xgboost.DMatrix(X_batch))
        for entry, pred in zip(data, predictions):
            if np.size(pred) == 0:
                logger.info("No prediction for %s, skipping", entry.timestamp)
                continue

            yield entry.timestamp, float(pred)

    def predict(self, source: InfluxHandle, model: Booster) -> None:
        data = self.query(source)
        results = self._predict_batch(list(data), model)
        write_to_influx(source, self.map_predictions(results))

    def train(self, source: InfluxHandle, target: InfluxHandle) -> None:
        data = self.compose(self.bundles(source))
        logger.info("Fetched and composed training data for program: %s.", self.program_name)
        logger.debug("First 5 elements: %s", data[:5])
        logger.info("Last element: %s", data[-1])
        logger.debug("Last element as line protocol: %s", data[-1].to_line_protocol())

        write_to_influx(target, data)
        logger.info("Stored training data to InfluxDB")

        # write all to influx, but omit 1.5 years of tail bank days from training
        # in order to force prediction over data yet unseen to the resulting model
        data = list(data[:-375])
        export_to_file(data, program_name=self.program_name)
        logger.info("Exported training data to S3")
