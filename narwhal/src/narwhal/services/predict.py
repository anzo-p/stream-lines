import logging
import os
import tarfile
import tempfile
from datetime import date, timedelta
from typing import List, Iterator

import boto3
import numpy as np
import xgboost
from xgboost import DMatrix, Booster

from narwhal.domain.constants import ALPACA_DATA_DAWN
from narwhal.domain.create_dataset import write_to_influx, fetch_training_data
from narwhal.domain.prediction_result import PredictionResult
from narwhal.domain.training_data import TrainingData
from narwhal.sinks.s3.export_file import S3_DATA_BUCKET, S3_MODEL_PREFIX
from narwhal.sources.influx.client import close_all_influx_clients, get_training_data_handle

logger = logging.getLogger(__name__)


class PredictionService:
    def __init__(self):
        self.s3 = boto3.client("s3")
        self.bucket = S3_DATA_BUCKET
        self.training_data_handle = get_training_data_handle()

    def _all_dates(self) -> Iterator[date]:
        today = date.today()
        for i in range((today - ALPACA_DATA_DAWN).days + 1):
            yield ALPACA_DATA_DAWN + timedelta(days=i)

    def _compose_model_file_name(self, model_id: str) -> str:
        return f"{S3_MODEL_PREFIX}/{model_id}/output/model.tar.gz"

    def _load_model(self, model_id: str) -> xgboost.Booster:
        with tempfile.TemporaryDirectory() as tmpdir:
            tar_path = os.path.join(tmpdir, "model.tar.gz")
            self.s3.download_file(self.bucket, self._compose_model_file_name(model_id), tar_path)

            with tarfile.open(tar_path, "r:gz") as tar:
                tar.extractall(path=tmpdir)

            model = xgboost.Booster()
            model_file = os.path.join(tmpdir, "xgboost-model")
            model.load_model(model_file)
            return model

    def run(self, model_id: str) -> None:
        results: List[PredictionResult] = []

        try:
            logger.info(f"Loading training data to predict over")
            prediction_data: List[TrainingData] = fetch_training_data(self.training_data_handle)

            logger.info(f"Loading model {model_id}")
            model: Booster = self._load_model(model_id)

            for entry in prediction_data:
                X = np.array(
                    [
                        [
                            d.members_daily_spread,
                            d.index_over_moving_avg,
                            d.index_over_kaufman_avg,
                            d.volume_over_moving_avg,
                            d.current_drawdown,
                            d.days_since_dip,
                        ]
                        for d in [entry]
                    ],
                    dtype=float,
                )

                logger.debug(f"Running xgboost.DMatrix on X with shape {X.shape}")
                dmatrix: DMatrix = xgboost.DMatrix(X)
                logger.debug(f"Running model.predict")
                prediction = model.predict(dmatrix)

                if prediction.size == 0:
                    logger.info("No prediction for date %s, skipping", entry.timestamp)
                    continue

                results.append(
                    PredictionResult(
                        timestamp=entry.timestamp, fwd_max_drawdown=float(prediction[0])
                    )
                )

            write_to_influx(results, self.training_data_handle)
            logger.info("Predictions completed successfully")
        except Exception as e:
            logger.error("Prediction failed: %s", e)

        finally:
            close_all_influx_clients()
