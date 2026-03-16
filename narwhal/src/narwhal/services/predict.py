import logging
import tempfile
from datetime import date
from typing import List, Optional

import xgboost
from xgboost import Booster, DMatrix

from narwhal.services.runners.drawdown_next_bank_day import DRAWDOWN_NEXT_BANK_DAY_RUNS
from narwhal.services.runners.forward_max_drawdown import FORWARD_MAX_DRAWDOWN_RUNS
from narwhal.sources.influx.client import close_all_influx_clients, get_training_data_handle
from narwhal.sources.s3.import_file import import_model_file

logger = logging.getLogger(__name__)


class PredictionService:
    def __init__(self) -> None:
        self.training_data_handle = get_training_data_handle()

    def _load_model(self, program_name: str, model_id: Optional[str] = None) -> xgboost.Booster:
        with tempfile.TemporaryDirectory() as tmpdir:
            model_file = import_model_file(tmpdir, program_name, model_id)
            model = xgboost.Booster()
            model.load_model(model_file)
            return model

    def run(self, program: Optional[str] = None, model_id: Optional[str] = None) -> None:
        runners = DRAWDOWN_NEXT_BANK_DAY_RUNS | FORWARD_MAX_DRAWDOWN_RUNS
        try:
            filtered_runners = (r for r in runners if not program or r.program_name == program)

            for runner in filtered_runners:
                logger.info(
                    f"Starting program: {runner.program_name} run for variant: {runner.variant}"
                )

                logger.info(f"Loading model {model_id}")
                model: Booster = self._load_model(runner.program_name, model_id)

                runner.predict(source=self.training_data_handle, model=model)
                logger.info(
                    f"Finished program: {runner.program_name} prediction run for variant: {runner.variant}"
                )

            logger.info("Predictions finished successfully.")
        except Exception as e:
            logger.error("Prediction failed: %s", e)

        finally:
            close_all_influx_clients()
