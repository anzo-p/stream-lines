import datetime
import logging
import os
from typing import Iterable

import boto3

from narwhal.domain.schema.training_fields_base import TrainingFieldsBase
from narwhal.sinks.s3.compose_csv import to_gzipped_csv

s3 = boto3.client("s3")


S3_DATA_BUCKET = os.environ["S3_DATA_BUCKET"]
S3_PREDICTION_PREFIX = os.environ["S3_PREDICTION_PREFIX"]
S3_TRAINING_PREFIX = os.environ["S3_TRAINING_PREFIX"]
S3_MODEL_PREFIX = os.environ["S3_MODEL_PREFIX"]
S3_MODEL_FILENAME = "xgboost-model"


def _resolve_filename() -> str:
    run_id = datetime.datetime.now(datetime.timezone.utc).strftime("%Y.%m.%dT:%H:%M:%SZ")
    return f"{run_id}.csv.gz"


def _latest() -> str:
    return "latest.csv.gz"


def _save_to_s3(content: bytes, path: str) -> None:
    logger = logging.getLogger(__name__)

    s3.put_object(
        Bucket=S3_DATA_BUCKET,
        Key=f"{S3_TRAINING_PREFIX}/{path}/{_resolve_filename()}",
        Body=content,
        ContentType="application/gzip",
    )
    logger.info(f"Exported {_resolve_filename()}")

    s3.put_object(
        Bucket=S3_DATA_BUCKET,
        Key=f"{S3_TRAINING_PREFIX}/{path}/{_latest()}",
        Body=content,
        ContentType="application/gzip",
    )
    logger.info(f"Exported {_latest()}")


def export_to_file(data: Iterable[TrainingFieldsBase], path: str) -> None:
    content = to_gzipped_csv(data)
    _save_to_s3(content, path)
