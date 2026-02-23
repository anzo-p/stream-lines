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
    def __init__(self) -> None:
        self.historical_data_handle = get_historical_data_handle()
        self.training_data_handle = get_training_data_handle()

    def run(self) -> None:
        logger = logging.getLogger(__name__)
        logger.info(f"Reading training data")

        try:
            training_data: List[TrainingData] = fetch_raw_data(self.historical_data_handle)
            write_to_influx(self.training_data_handle, training_data)
            # write it all to influx, but omit 1.5 years worth of tail bank days
            # in order to force prediction over data yet unseen to the model
            training_data = list(training_data[:-375])
            content: bytes = to_gzipped_csv(training_data)
            export_training_file(content)

            logger.info(f"Training data successfully composed and exported")
        except Exception as e:
            logger.error(f"Failed to export training data: {e}")

        finally:
            close_all_influx_clients()
