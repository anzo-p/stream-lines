import os
from dataclasses import dataclass
from typing import Dict, Tuple

from influxdb_client import InfluxDBClient
from influxdb_client.client.query_api import QueryApi


@dataclass(frozen=True)
class InfluxHandle:
    org: str
    bucket: str
    query_api: QueryApi


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


def get_historical_data() -> InfluxHandle:
    token = os.environ["INFLUXDB_TOKEN_HISTORICAL_READ"]
    bucket = os.environ["INFLUXDB_BUCKET_MARKET_DATA_HISTORICAL"]

    client = _get_client(_influx_url, _influx_org, token)
    return InfluxHandle(org=_influx_org, bucket=bucket, query_api=client.query_api())


def get_realtime() -> InfluxHandle:
    token = os.environ["INFLUXDB_TOKEN_REALTIME_READ"]
    bucket = os.environ["INFLUXDB_BUCKET_MARKET_DATA_REALTIME"]

    client = _get_client(_influx_url, _influx_org, token)
    return InfluxHandle(org=_influx_org, bucket=bucket, query_api=client.query_api())


def close_all_influx_clients() -> None:
    for c in _clients.values():
        c.close()
    _clients.clear()
