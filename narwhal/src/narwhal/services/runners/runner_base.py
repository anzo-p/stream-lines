import logging
from dataclasses import dataclass
from datetime import date
from typing import Generic, Iterable, TypeVar

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

    @property
    def program(self) -> str:
        raise NotImplementedError

    def bundles(self, h: InfluxHandle) -> Iterable[T]:
        raise NotImplementedError

    def compose(self, bundles: Iterable[T]) -> list[U]:
        raise NotImplementedError

    def map_predictions(self, predictions: Iterable[tuple[date, float]]) -> list[V]:
        raise NotImplementedError

    def query(self, h: InfluxHandle) -> Iterable[U]:
        raise NotImplementedError

    def run(self, source: InfluxHandle, target: InfluxHandle) -> None:
        data: list[U] = self.compose(self.bundles(source))
        logger.info("Fetched and composed training data for program: %s.", self.program)
        logger.debug("First 5 elements: %s", data[:5])

        write_to_influx(target, data)
        logger.info("Stored training data to InfluxDB")
        # write all to influx, but omit 1.5 years of tail bank days from training
        # in order to force prediction over data yet unseen to the resulting model
        data = list(data[:-375])

        export_to_file(data, path=self.program)
        logger.info("Exported training data to S3")

    def store_predictions(self, h: InfluxHandle, predictions: Iterable[Serializable]) -> None:
        write_to_influx(h, predictions)
