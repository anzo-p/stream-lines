import logging
from typing import Iterator, List

from narwhal.domain.schema.drawdown.training_fields import DrawdownTrainingFields
from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.helpers import compose_default_range


def _flux(bucket: str, variant: str) -> str:
    return f'''
from(bucket: "{bucket}")
  |> {compose_default_range()}
  |> filter(fn: (r) => r._measurement == "drawdown-training-data-{variant}")
  |> pivot(
      rowKey:["_time"],
      columnKey: ["_field"],
      valueColumn: "_value"
  )
'''.strip()


def query(h: InfluxHandle, variant: str) -> Iterator[DrawdownTrainingFields]:
    logger = logging.getLogger(__name__)

    table_list = h.query_api.query(_flux(h.bucket, variant))

    out: List[DrawdownTrainingFields] = []
    for table in table_list:
        for record in table.records:
            out.append(
                DrawdownTrainingFields(
                    timestamp=record["_time"].date(),
                    fwd_max_drawdown=record["fwd_max_drawdown"],
                    members_daily_spread=record["members_daily_spread"],
                    index_over_moving_avg=record["index_over_moving_avg"],
                    index_over_kaufman_avg=record["index_over_kaufman_avg"],
                    volume_over_moving_avg=record["volume_over_moving_avg"],
                    current_drawdown=record["current_drawdown"],
                    days_since_dip_of_3=record["days_since_dip_of_3"],
                    days_since_dip_of_5=record["days_since_dip_of_5"],
                    days_since_dip_of_8=record["days_since_dip_of_8"],
                    days_since_dip_of_13=record["days_since_dip_of_13"],
                    vix=record["vix"],
                    variant="two-weeks",
                )
            )

    logger.info(f"Processed {len(out)} drawdown training data from InfluxDB")

    yield from out
