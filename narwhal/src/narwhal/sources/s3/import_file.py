import os
import tarfile
from typing import Optional

import boto3


S3_DATA_BUCKET = os.environ["S3_DATA_BUCKET"]
S3_MODELS_LATEST_KEY = os.environ["S3_MODELS_LATEST_KEY"]
S3_MODELS_RUNS_KEY = os.environ["S3_MODELS_RUNS_KEY"]
S3_MODEL_FILENAME = "xgboost-model"


_s3 = boto3.client("s3")


def _compose_fullpath_by_model_id(program_name: str, model_id: str) -> str:
    return f"{S3_MODELS_RUNS_KEY}/{program_name}/{model_id}/output/model.tar.gz"


def _compose_fullpath_for_latest(program_name: str) -> str:
    return f"{S3_MODELS_LATEST_KEY}/{program_name}/model.tar.gz"


def import_model_file(target_dir: str, program_name: str, model_id: Optional[str] = None) -> str:
    tar_path = os.path.join(target_dir, "model.tar.gz")

    s3_key = (
        _compose_fullpath_by_model_id(program_name, model_id)
        if model_id
        else _compose_fullpath_for_latest(program_name)
    )

    _s3.download_file(
        S3_DATA_BUCKET,
        s3_key,
        tar_path,
    )

    with tarfile.open(tar_path, "r:gz") as tar:
        tar.extractall(path=target_dir)

    return os.path.join(target_dir, S3_MODEL_FILENAME)
