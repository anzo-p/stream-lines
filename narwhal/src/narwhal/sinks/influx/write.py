import logging
from itertools import islice
from typing import Iterable, TypeVar

from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.serializable import Serializable


T = TypeVar("T")


logger = logging.getLogger(__name__)


def _batched(iterable: Iterable[T], size: int) -> Iterable[list[T]]:
    it = iter(iterable)
    while batch := list(islice(it, size)):
        yield batch


def write_to_influx(h: InfluxHandle, data: Iterable[Serializable], batch_size: int = 100) -> None:
    if h.write_api is None:
        raise RuntimeError("write_api is not initialized")

    for batch in _batched(data, batch_size):
        try:
            payload = "\n".join(d.to_line_protocol() for d in batch)
            h.write_api.write(
                bucket=h.bucket,
                org=h.org,
                record=payload,
            )
        except Exception:
            logger.exception("Failed writing batch to InfluxDB", extra={"batch_size": len(batch)})
