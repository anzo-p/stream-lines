import logging

from narwhal.domain.constants import BANK_DAYS_OF_TWO_MONTHS, BANK_DAYS_OF_TWO_WEEKS
from narwhal.domain.schema.day_bundle import DayBundleSchema
from narwhal.domain.schema.field_extractors import FIELD_EXTRACTORS
from narwhal.domain.schema.training_data import TrainingData
from narwhal.sources.influx.client import InfluxHandle
from narwhal.sources.influx.drawdown import drawdown_query
from narwhal.sources.influx.index_data import index_query
from narwhal.sources.influx.members import member_query
from narwhal.sources.influx.vix import vix_query
from narwhal.sources.influx.volume import volume_query

logger = logging.getLogger(__name__)


def _compose_row(bundle: DayBundleSchema) -> TrainingData:
    kwargs = {name: fn(bundle) for name, fn in FIELD_EXTRACTORS.items()}
    return TrainingData(**kwargs)


def compose_training_data(h: InfluxHandle) -> list[TrainingData]:
    data = DayBundleSchema.daily_generator(
        drawdown=drawdown_query(h, BANK_DAYS_OF_TWO_WEEKS),
        index=index_query(h, BANK_DAYS_OF_TWO_MONTHS),
        members=member_query(h),
        vix=vix_query(h),
        volume=volume_query(h, BANK_DAYS_OF_TWO_MONTHS),
    )
    out: list[TrainingData] = [_compose_row(r) for r in data]

    logger.debug("Combined members (first 5): %s", out[:5])
    return out
