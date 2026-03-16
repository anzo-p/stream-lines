import logging

from narwhal.services.runners.drawdown_next_bank_day import DRAWDOWN_NEXT_BANK_DAY_RUNS
from narwhal.services.runners.forward_max_drawdown import FORWARD_MAX_DRAWDOWN_RUNS
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
        runners = DRAWDOWN_NEXT_BANK_DAY_RUNS | FORWARD_MAX_DRAWDOWN_RUNS
        try:
            for runner in runners:
                logger.info(
                    f"Starting program: {runner.program_name} run for variant: {runner.variant}"
                )
                runner.train(
                    source=self.historical_data_handle,
                    target=self.training_data_handle,
                )
                logger.info(
                    f"Finished program: {runner.program_name} run for variant: {runner.variant}"
                )

            logger.info("Training data compilation and export finished successfully.")
        except Exception as e:
            logger.error(f"Failed to export training data: {e}")

        finally:
            close_all_influx_clients()
