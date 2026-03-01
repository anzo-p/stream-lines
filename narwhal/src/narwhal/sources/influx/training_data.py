import logging
from typing import Iterator, List

from narwhal.domain.schema.drawdown.training_data import DrawdownTrainingData
from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.helpers import compose_default_range


def _flux(bucket: str) -> str:
    return f'''
from(bucket: "{bucket}")
  |> {compose_default_range()}
  |> filter(fn: (r) => r._measurement == "drawdown-training-data")
  |> pivot(
      rowKey:["_time"],
      columnKey: ["_field"],
      valueColumn: "_value"
  )
'''.strip()


def training_data_query(h: InfluxHandle) -> Iterator[DrawdownTrainingData]:
    logger = logging.getLogger(__name__)

    table_list = h.query_api.query(_flux(h.bucket))

    out: List[DrawdownTrainingData] = []
    for table in table_list:
        for record in table.records:
            out.append(
                DrawdownTrainingData(
                    timestamp=record["_time"].date(),
                    fwd_max_drawdown=record["fwd_max_drawdown"],
                    members_daily_spread=record["members_daily_spread"],
                    index_over_moving_avg=record["index_over_moving_avg"],
                    index_over_kaufman_avg=record["index_over_kaufman_avg"],
                    volume_over_moving_avg=record["volume_over_moving_avg"],
                    current_drawdown=record["current_drawdown"],
                    days_since_dip=record["days_since_dip"],
                    vix=record["vix"],
                )
            )

    logger.info(f"Processed {len(out)} drawdown training data from InfluxDB")

    yield from out
