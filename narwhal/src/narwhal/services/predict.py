import logging
import tempfile
from typing import Iterator, List, Optional

import xgboost
from xgboost import Booster, DMatrix

from narwhal.domain.schema.prediction_result import PredictionResult
from narwhal.domain.schema.training_data import TrainingData
from narwhal.sinks.influx.write import write_to_influx
from narwhal.sources.influx.client import close_all_influx_clients, get_training_data_handle
from narwhal.sources.influx.training_data import training_data_query
from narwhal.sources.s3.import_file import import_model_file

logger = logging.getLogger(__name__)


class PredictionService:
    def __init__(self) -> None:
        self.training_data_handle = get_training_data_handle()

    def _load_model(self, model_id: str) -> xgboost.Booster:
        with tempfile.TemporaryDirectory() as tmpdir:
            model_file = import_model_file(tmpdir, model_id)
            model = xgboost.Booster()
            model.load_model(model_file)
            return model

    def run(self, model_id: Optional[str] = None) -> None:
        model_id = model_id or "latest"
        results: List[PredictionResult] = []

        try:
            logger.info(f"Loading training data to predict over")
            prediction_data: Iterator[TrainingData] = training_data_query(self.training_data_handle)

            logger.info(f"Loading model {model_id}")
            model: Booster = self._load_model(model_id)

            for entry in prediction_data:
                X = entry.x_vector()
                logger.debug(f"Running xgboost.DMatrix on X with shape {X.shape}")
                d_matrix: DMatrix = xgboost.DMatrix(X)
                logger.debug(f"Running model.predict")
                prediction = model.predict(d_matrix)

                if prediction.size == 0:
                    logger.info("No prediction for date %s, skipping", entry.timestamp)
                    continue

                results.append(
                    PredictionResult(
                        timestamp=entry.timestamp, fwd_max_drawdown=float(prediction[0])
                    )
                )

            write_to_influx(self.training_data_handle, results)
            logger.info("Predictions completed successfully")
        except Exception as e:
            logger.error("Prediction failed: %s", e)

        finally:
            close_all_influx_clients()
