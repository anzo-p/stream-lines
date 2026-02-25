import os
import tarfile

import boto3

from narwhal.sinks.s3.export_file import S3_MODEL_PREFIX, S3_DATA_BUCKET, S3_MODEL_FILENAME

_s3 = boto3.client("s3")


def _compose_model_file_name(model_id: str) -> str:
    return f"{S3_MODEL_PREFIX}/{model_id}/output/model.tar.gz"


def import_model_file(target_dir: str, model_id: str) -> str:
    tar_path = os.path.join(target_dir, "model.tar.gz")

    _s3.download_file(
        S3_DATA_BUCKET,
        _compose_model_file_name(model_id),
        tar_path,
    )

    with tarfile.open(tar_path, "r:gz") as tar:
        tar.extractall(path=target_dir)

    return os.path.join(target_dir, S3_MODEL_FILENAME)
