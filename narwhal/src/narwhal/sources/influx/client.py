import json
import os
from dataclasses import dataclass
from typing import Dict, Tuple, Optional

import boto3
from influxdb_client import InfluxDBClient, WriteApi
from influxdb_client.client.query_api import QueryApi
from influxdb_client.client.write_api import SYNCHRONOUS
from mypy_boto3_secretsmanager.client import SecretsManagerClient


@dataclass(frozen=True)
class InfluxHandle:
    org: str
    bucket: str
    query_api: QueryApi
    write_api: Optional[WriteApi]


_influx_org = os.environ["INFLUXDB_ORG"]
_influx_url = os.environ["INFLUXDB_URL"]
_clients: Dict[Tuple[str, str, str], InfluxDBClient] = {}


def _get_client(url: str, org: str, token: str) -> InfluxDBClient:
    key = (url, org, token)
    c = _clients.get(key)
    if c is None:
        c = InfluxDBClient(url=url, token=token, org=org, timeout=120_000)
        _clients[key] = c
    return c


def _get_query_api(token: str) -> QueryApi:
    client = _get_client(_influx_url, _influx_org, token)
    return client.query_api()


def _get_token(secret_name: str, key: str) -> str:
    client: SecretsManagerClient = boto3.client("secretsmanager", os.environ["AWS_REGION"])
    response = client.get_secret_value(SecretId=secret_name)
    secret: dict[str, str] = json.loads(response["SecretString"] or "{}")

    if key not in secret:
        raise KeyError(f"Key '{key}' not found in secret '{secret_name}'")

    return secret[key]


def _get_write_api(token: str) -> WriteApi:
    client = _get_client(_influx_url, _influx_org, token)
    return client.write_api(
        timeout=60_000,
        write_options=SYNCHRONOUS,  # mostly scheduled tasks, must wait until signaling ready
    )


def _get_handle(bucket: str, token: str, write: bool = False) -> InfluxHandle:

    return InfluxHandle(
        org=_influx_org,
        bucket=bucket,
        query_api=_get_query_api(token),
        write_api=_get_write_api(token) if write else None,
    )


def get_historical_data_handle() -> InfluxHandle:
    return _get_handle(
        bucket=os.environ["INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL"],
        token=_get_token("prod/influxdb/market-data-historical/read", "influxdb.token"),
    )


def get_realtime_data_handle() -> InfluxHandle:
    return _get_handle(
        bucket=os.environ["INFLUXDB_BUCKET_MARKET_DATA_REALTIME"],
        token=_get_token("prod/influxdb/market-data-realtime/read", "influxdb.token"),
    )


def get_training_data_handle() -> InfluxHandle:
    return _get_handle(
        bucket=os.environ["INFLUXDB_BUCKET_TRAINING_DATA"],
        token=_get_token("prod/influxdb/training-data/read-write", "influxdb.token"),
        write=True,
    )


def close_all_influx_clients() -> None:
    for c in _clients.values():
        c.close()
    _clients.clear()
