import logging
from typing import List

from narwhal.domain.create_dataset import fetch_raw_data, write_to_influx
from narwhal.domain.training_data import TrainingData
from narwhal.sinks.s3.compose_csv import to_gzipped_csv
from narwhal.sinks.s3.export_file import export_training_file
from narwhal.sources.influx.client import (
    get_historical_data_handle,
    close_all_influx_clients,
    get_training_data_handle,
)


logger = logging.getLogger(__name__)


class TrainingService:
    def __init__(self):
        self.historical_data_handle = get_historical_data_handle()
        self.training_data_handle = get_training_data_handle()

    def run(self) -> None:
        logger = logging.getLogger(__name__)
        logger.info(f"Reading training data")

        try:
            training_data: List[TrainingData] = fetch_raw_data(self.historical_data_handle)
            # forces to predict over unseen future by omitting 1.5 years worth of tail bank days
            training_data = list(training_data[:-375])
            write_to_influx(training_data, self.training_data_handle)

            content: bytes = to_gzipped_csv(training_data)
            export_training_file(content)

            logger.info(f"Training data successfully composed and exported")
        except Exception as e:
            logger.error(f"Failed to export training data: {e}")

        finally:
            close_all_influx_clients()
