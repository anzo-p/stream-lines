from dataclasses import dataclass

from narwhal.domain.schema.daily_bundle_base import DayBundleBase
from narwhal.sources.influx.market_data.drawdown import DrawdownData
from narwhal.sources.influx.market_data.index_data import IndexData
from narwhal.sources.influx.market_data.members import MemberData
from narwhal.sources.influx.market_data.vix import VixData
from narwhal.sources.influx.market_data.volume import VolumeData


@dataclass(frozen=True)
class ForwardMaxDrawdownDays(DayBundleBase):
    drawdown: DrawdownData
    index: IndexData
    members: MemberData
    vix: VixData
    volume: VolumeData
