import logging

from narwhal.domain.constants import BANK_DAYS_OF_TWO_MONTHS, BANK_DAYS_OF_TWO_WEEKS
from narwhal.domain.schema.drawdown.day_bundle import DrawdownDayBundle
from narwhal.domain.schema.drawdown.training_data import DrawdownTrainingData
from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.drawdown import drawdown_query
from narwhal.sources.influx.index_data import index_query
from narwhal.sources.influx.members import member_query
from narwhal.sources.influx.vix import vix_query
from narwhal.sources.influx.volume import volume_query

logger = logging.getLogger(__name__)


"""
Module provides means to compose datasets for training and prediction

Main concepts
-------------
- A composer must essentially satisfy: DayBundle -> TrainingData
- Both of the above types will know their required fields
- Neither type needs to know of db handles or how to obtain values to those fields
"""


def compose_drawdown_training_data(h: InfluxHandle) -> list[DrawdownTrainingData]:
    data = DrawdownDayBundle.daily_generator(
        drawdown=drawdown_query(h, BANK_DAYS_OF_TWO_WEEKS),
        index=index_query(h, BANK_DAYS_OF_TWO_MONTHS),
        members=member_query(h),
        vix=vix_query(h),
        volume=volume_query(h, BANK_DAYS_OF_TWO_MONTHS),
    )
    out: list[DrawdownTrainingData] = [DrawdownTrainingData.compose_row(r) for r in data]

    logger.debug("Combined members (first 5): %s", out[:5])
    return out
