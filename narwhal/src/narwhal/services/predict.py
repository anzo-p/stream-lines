import logging
import tempfile
from datetime import date
from typing import List, Optional

import xgboost
from xgboost import Booster, DMatrix

from narwhal.services.runners.drawdown_runner import DRAWDOWN_RUNS
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
        try:
            filtered_runners = (
                r for r in DRAWDOWN_RUNS if not program or r.program_name == program
            )

            for runner in filtered_runners:
                logger.info("Starting drawdown prediction run for variant: %s", runner.variant)

                logger.info(f"Loading model {model_id}")
                model: Booster = self._load_model(runner.program_name, model_id)

                runner.predict(source=self.training_data_handle, model=model)
                logger.info("Finished drawdown prediction run for variant: %s", runner.variant)

        except Exception as e:
            logger.error("Prediction failed: %s", e)

        finally:
            close_all_influx_clients()
