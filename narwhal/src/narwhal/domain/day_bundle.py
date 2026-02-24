from dataclasses import dataclass
from datetime import date

from narwhal.sources.influx.drawdown import DrawdownData
from narwhal.sources.influx.index_data import IndexData
from narwhal.sources.influx.members import MemberData
from narwhal.sources.influx.vix import VixData
from narwhal.sources.influx.volume import VolumeData


@dataclass(frozen=True)
class DayBundle:
    day: date
    index: IndexData
    drawdown: DrawdownData
    members: MemberData
    vix: VixData
    volume: VolumeData
