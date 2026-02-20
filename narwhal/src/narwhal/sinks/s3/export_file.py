import datetime
import logging
import os

import boto3

s3 = boto3.client("s3")


S3_DATA_BUCKET = os.environ["S3_DATA_BUCKET"]
S3_PREDICTION_PREFIX = os.environ["S3_PREDICTION_PREFIX"]
S3_TRAINING_PREFIX = os.environ["S3_TRAINING_PREFIX"]
S3_MODEL_PREFIX = os.environ["S3_MODEL_PREFIX"]


def _resolve_filename() -> str:
    run_id = datetime.datetime.now(datetime.timezone.utc).strftime("%Y.%m.%dT:%H:%M:%SZ")
    return f"data_{run_id}.csv.gz"


def _latest() -> str:
    return f"data_latest.csv.gz"


def export_training_file(content: bytes):
    logger = logging.getLogger(__name__)

    s3 = boto3.client("s3")

    s3.put_object(
        Bucket=S3_DATA_BUCKET,
        Key=f"{S3_TRAINING_PREFIX}/train_{_resolve_filename()}",
        Body=content,
        ContentType="application/gzip",
    )
    logger.info(f"Exported {_resolve_filename()}")

    s3.put_object(
        Bucket=S3_DATA_BUCKET,
        Key=f"{S3_TRAINING_PREFIX}/train_{_latest()}",
        Body=content,
        ContentType="application/gzip",
    )
    logger.info(f"Exported {_latest()}")
