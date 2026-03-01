from dataclasses import dataclass

from narwhal.domain.schema.daily_bundle_base import DayBundleBase
from narwhal.sources.influx.drawdown import DrawdownData
from narwhal.sources.influx.index_data import IndexData
from narwhal.sources.influx.members import MemberData
from narwhal.sources.influx.vix import VixData
from narwhal.sources.influx.volume import VolumeData


@dataclass(frozen=True)
class DrawdownDayBundle(DayBundleBase):
    drawdown: DrawdownData
    index: IndexData
    members: MemberData
    vix: VixData
    volume: VolumeData
