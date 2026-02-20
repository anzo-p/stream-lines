import os
from dataclasses import dataclass
from typing import Dict, Tuple, Optional

from influxdb_client import InfluxDBClient, WriteApi
from influxdb_client.client.query_api import QueryApi


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


def _get_write_api(token: str) -> WriteApi:
    client = _get_client(_influx_url, _influx_org, token)
    return client.write_api(timeout=60_000)


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
        token=os.environ["INFLUXDB_TOKEN_HISTORICAL_READ"],
    )


def get_realtime_data_handle() -> InfluxHandle:
    return _get_handle(
        bucket=os.environ["INFLUXDB_BUCKET_MARKET_DATA_REALTIME"],
        token=os.environ["INFLUXDB_TOKEN_REALTIME_READ"],
    )


def get_training_data_handle() -> InfluxHandle:
    return _get_handle(
        bucket=os.environ["INFLUXDB_BUCKET_TRAINING_DATA"],
        token=os.environ["INFLUXDB_TOKEN_TRAINING_DATA_READ_WRITE"],
        write=True,
    )


def close_all_influx_clients() -> None:
    for c in _clients.values():
        c.close()
    _clients.clear()
