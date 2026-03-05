import logging

from narwhal.services.runners.drawdown_runner import DRAWDOWN_RUNS
from narwhal.sources.influx.client import (
    close_all_influx_clients,
    get_historical_data_handle,
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
            for runner in DRAWDOWN_RUNS:
                logger.info("Starting drawdown run for variant: %s", runner.variant)
                runner.run(
                    source=self.historical_data_handle,
                    target=self.training_data_handle,
                )
                logger.info("Finished drawdown run for variant: %s", runner.variant)

            logger.info(f"Training data successfully composed and exported")
        except Exception as e:
            logger.error(f"Failed to export training data: {e}")

        finally:
            close_all_influx_clients()
