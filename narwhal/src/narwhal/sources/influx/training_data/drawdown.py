import logging
from typing import Iterator, List

from narwhal.domain.schema.drawdown_next_bank_day.training_fields import DrawdownNextBankDayFields
from narwhal.domain.schema.forward_max_drawdown.training_fields import ForwardMaxDrawdownFields
from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.helpers import compose_default_range

logger = logging.getLogger(__name__)


def _flux(bucket: str, measurement: str) -> str:
    return f'''
from(bucket: "{bucket}")
  |> {compose_default_range()}
  |> filter(fn: (r) => r._measurement == "{measurement}")
  |> pivot(
      rowKey:["_time"],
      columnKey: ["_field"],
      valueColumn: "_value"
  )
'''.strip()


def query_drawdown_next_bank_day(h: InfluxHandle) -> Iterator[DrawdownNextBankDayFields]:
    table_list = h.query_api.query(_flux(h.bucket, "drawdown-next-bank-day-training-data-"))

    out: List[DrawdownNextBankDayFields] = []
    for table in table_list:
        for record in table.records:
            out.append(
                DrawdownNextBankDayFields(
                    timestamp=record["_time"].date(),
                    drawdown_next_bank_day=record["drawdown_next_bank_day"],
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
                    variant="",
                )
            )

    logger.info(f"Processed {len(out)} drawdown next bank day training data from InfluxDB")

    yield from out


def query_forward_max_drawdown(h: InfluxHandle, variant: str) -> Iterator[ForwardMaxDrawdownFields]:
    table_list = h.query_api.query(_flux(h.bucket, f"forward-max-drawdown-training-data-{variant}"))

    out: List[ForwardMaxDrawdownFields] = []
    for table in table_list:
        for record in table.records:
            out.append(
                ForwardMaxDrawdownFields(
                    timestamp=record["_time"].date(),
                    forward_max_drawdown=record["forward_max_drawdown"],
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
                    variant=variant,
                )
            )

    logger.info(
        f"Processed {len(out)} forward max drawdown drawdown {variant} training data from InfluxDB"
    )

    yield from out
