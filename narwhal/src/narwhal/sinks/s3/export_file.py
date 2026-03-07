import datetime
import logging
import os
from typing import Iterable

import boto3

from narwhal.domain.schema.training_fields_base import TrainingFieldsBase
from narwhal.sinks.s3.compose_csv import to_gzipped_csv


S3_DATA_BUCKET = os.environ["S3_DATA_BUCKET"]
S3_TRAINING_KEY = os.environ["S3_TRAINING_KEY"]


_s3 = boto3.client("s3")


def _resolve_filename() -> str:
    run_id = datetime.datetime.now(datetime.timezone.utc).strftime("%Y.%m.%dT:%H:%M:%SZ")
    return f"{run_id}.csv.gz"


def _latest() -> str:
    return "latest.csv.gz"


def _save_to_s3(content: bytes, program_name: str) -> None:
    logger = logging.getLogger(__name__)

    _s3.put_object(
        Bucket=S3_DATA_BUCKET,
        Key=f"{S3_TRAINING_KEY}/{program_name}/{_resolve_filename()}",
        Body=content,
        ContentType="application/gzip",
    )
    logger.info(f"Exported {_resolve_filename()}")

    _s3.put_object(
        Bucket=S3_DATA_BUCKET,
        Key=f"{S3_TRAINING_KEY}/{program_name}/{_latest()}",
        Body=content,
        ContentType="application/gzip",
    )
    logger.info(f"Exported {_latest()}")


def export_to_file(data: Iterable[TrainingFieldsBase], program_name: str) -> None:
    content = to_gzipped_csv(data)
    _save_to_s3(content, program_name)
